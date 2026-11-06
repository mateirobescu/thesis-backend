package com.mateirobescu.thesis.users;

import com.mateirobescu.thesis.exception.ConflictException;
import com.mateirobescu.thesis.exception.ConstraintViolations;
import com.mateirobescu.thesis.exception.NotFoundException;
import com.mateirobescu.thesis.support.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks, against a real PostgreSQL with the Flyway migrations applied, that the unique index on a user's
 * lower-cased email and the version column behave the way {@code UserService} assumes. It lives in the
 * {@code users} package because {@code UserRepository} is package-private.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UserIntegrationTest {

    private static final String EMAIL = "john.doe@example.com";

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
    }

    private UserCreateCommand createCommand(String email) {
        return new UserCreateCommand(email, "hash", "John", "Doe");
    }

    private User userWithEmail(String email) {
        return User.builder().email(email).passwordHash("hash").firstName("John").lastName("Doe").build();
    }

    private User createVerifiedUser(UserCreateCommand command) {
        User user = userService.createUser(command);
        userService.verifyUserEmail(user);
        return user;
    }

    @Test
    void should_reportTheEmailConstraint_whenTwoActiveUsersShareAnEmail() {
        userRepository.saveAndFlush(userWithEmail(EMAIL));

        DataIntegrityViolationException error = assertThrows(DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(userWithEmail(EMAIL)));

        assertTrue(ConstraintViolations.isViolationOf(error, UserConstraints.EMAIL_UNIQUE_CONSTRAINT));
    }

    @Test
    void should_reportTheEmailConstraint_whenEmailsDifferOnlyByCase() {
        userRepository.saveAndFlush(userWithEmail(EMAIL));

        DataIntegrityViolationException error = assertThrows(DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(userWithEmail(EMAIL.toUpperCase())));

        assertTrue(ConstraintViolations.isViolationOf(error, UserConstraints.EMAIL_UNIQUE_CONSTRAINT));
    }

    @Test
    void should_throwConflict_whenUserIsCreatedWithAnExistingEmail() {
        userService.createUser(createCommand(EMAIL));

        assertThrows(ConflictException.class, () -> userService.createUser(createCommand(EMAIL)));
    }

    @Test
    void should_throwConflict_whenUserIsCreatedWithAnExistingEmailInOtherCase() {
        userService.createUser(createCommand(EMAIL));

        assertThrows(ConflictException.class, () -> userService.createUser(createCommand(EMAIL.toUpperCase())));
    }

    //TODO maybe there should be a cooldown on this behaviour? or just revive that user
    @Test
    void should_allowTheEmail_whenTheUserWithItIsSoftDeleted() {
        User first = userService.createUser(createCommand(EMAIL));
        userService.verifyUserEmail(first);
        userService.deleteUserById(first.getId());

        User second = assertDoesNotThrow(() -> userService.createUser(createCommand(EMAIL)));

        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void should_rejectTheStaleSave_whenUserIsSavedFromAnOldCopy() {
        User created = userService.createUser(createCommand(EMAIL));
        User first = userRepository.findById(created.getId()).orElseThrow();
        User second = userRepository.findById(created.getId()).orElseThrow();
        userRepository.save(first.markDeleted());

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> userRepository.save(second.markDeleted()));
    }

    @Test
    void should_throwNotFound_whenUserByIdIsSoftDeleted() {
        User created = createVerifiedUser(createCommand(EMAIL));
        userService.deleteUserById(created.getId());

        assertThrows(NotFoundException.class, () -> userService.getUserById(created.getId()));
    }

    @Test
    void should_returnEmpty_whenUserFoundByEmailIsSoftDeleted() {
        User created = createVerifiedUser(createCommand(EMAIL));
        userService.deleteUserById(created.getId());

        assertTrue(userService.findUserByEmail(EMAIL).isEmpty());
    }

    @Test
    void should_excludeTheUser_fromAllUsers_whenSoftDeleted() {
        User deleted = createVerifiedUser(createCommand(EMAIL));
        User kept = createVerifiedUser(new UserCreateCommand("jane.doe@example.com", "hash", "Jane", "Doe"));
        userService.deleteUserById(deleted.getId());

        assertEquals(List.of(kept.getId()), userService.getAllUsers().stream().map(User::getId).toList());
    }

    @Test
    void should_throwNotFound_whenUserByIdHasNotVerifiedTheEmail() {
        User created = userService.createUser(createCommand(EMAIL));

        assertThrows(NotFoundException.class, () -> userService.getUserById(created.getId()));
    }

    @Test
    void should_excludeTheUser_fromAllUsers_whenEmailIsNotVerified() {
        userService.createUser(createCommand(EMAIL));

        assertTrue(userService.getAllUsers().isEmpty());
    }

    @Test
    void should_createOneUserAndConflictTheRest_whenManyCreatesRaceForTheSameEmail() throws Exception {
        int threads = 10;
        UserCreateCommand command = createCommand(EMAIL);
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<User>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return userService.createUser(command);
                }));
            }

            ready.await();
            start.countDown();

            int created = 0;
            for (Future<User> future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS);
                    created++;
                } catch (ExecutionException e) {
                    assertInstanceOf(ConflictException.class, e.getCause());
                }
            }

            assertEquals(1, created);
            assertEquals(1, jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM users WHERE lower(email) = ? AND deleted_at IS NULL", Integer.class, EMAIL));
        }
    }
}
