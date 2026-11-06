package com.mateirobescu.thesis.users;

import com.mateirobescu.thesis.exception.ConflictException;
import com.mateirobescu.thesis.exception.NotFoundException;
import com.mateirobescu.thesis.support.MockDatabaseException;
import org.instancio.Instancio;
import org.instancio.InstancioApi;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private UserService service;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    //TODO maybe should be simpler
    private static InstancioApi<User> randomUserBuilder() {
        return Instancio.of(User.class).generate(field(User::getEmail), gen -> gen.text().pattern("#h.#h" +
                "#h#h@example.com")).set(field(User::getDeletedAt), null);
    }

    private static User randomUser() {
        return UserServiceTest.randomUserBuilder().create();
    }

    @Nested
    class CreateUser {

        @Test
        void should_returnUser_whenCommandIsValid() {
            UserCreateCommand command = Instancio.create(UserCreateCommand.class);
            User persisted = randomUser();

            when(repository.findByEmailAndDeletedAtIsNull(command.email())).thenReturn(Optional.empty());
            when(repository.save(any(User.class))).thenReturn(persisted);

            User result = service.createUser(command);

            assertSame(persisted, result);

            verify(repository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertEquals(command.email(), saved.getEmail());
            assertEquals(command.passwordHash(), saved.getPasswordHash());
            assertEquals(command.firstName(), saved.getFirstName());
            assertEquals(command.lastName(), saved.getLastName());
            assertNull(saved.getDeletedAt());
            assertNull(saved.getEmailVerifiedAt());
        }

        @Test
        void should_throwConflict_whenUserWithSameEmailAlreadyExists() {
            UserCreateCommand command = Instancio.create(UserCreateCommand.class);
            when(repository.findByEmailAndDeletedAtIsNull(command.email()))
                    .thenReturn(Optional.of(User.builder().email(command.email()).build()));

            assertThatThrownBy(() -> service.createUser(command))
                    .isExactlyInstanceOf(ConflictException.class)
                    .hasMessageContaining("An account with this email already exists")
                    .extracting(e -> ((ConflictException) e).getField()).isEqualTo("email");

            verify(repository, never()).save(any());
        }

        @Test
        void should_throwConflict_whenUserWithSameEmailWasCreatedDuringThisCreation() {
            UserCreateCommand command = Instancio.create(UserCreateCommand.class);
            when(repository.findByEmailAndDeletedAtIsNull(command.email()))
                    .thenReturn(Optional.empty());
            when(repository.save(any(User.class))).thenThrow(MockDatabaseException.constraintViolation(UserConstraints.EMAIL_UNIQUE_CONSTRAINT));

            assertThatThrownBy(() -> service.createUser(command))
                    .isExactlyInstanceOf(ConflictException.class)
                    .hasMessageContaining("An account with this email already exists")
                    .extracting(e -> ((ConflictException) e).getField()).isEqualTo("email");

        }

    }

    @Nested
    class GetUserById {

        @Test
        void should_returnUser_whenUserExistsAndIsActive() {
            User user = randomUser();
            when(repository.findByIdAndDeletedAtIsNullAndEmailVerifiedAtIsNotNull(user.getId()))
                    .thenReturn(Optional.of(user));

            User result = service.getUserById(user.getId());
            assertSame(user, result);
        }

        @Test
        void should_throwNotFound_whenUserDoesntExist() {
            UUID userId = UUID.randomUUID();
            when(repository.findByIdAndDeletedAtIsNullAndEmailVerifiedAtIsNotNull(userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getUserById(userId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(userId.toString());
        }
    }

    @Nested
    class FindByEmail {
        @Test
        void should_returnUserOptional_whenUserExistsAndIsActive() {
            User user = randomUser();
            when(repository.findByEmailAndDeletedAtIsNull(user.getEmail())).thenReturn(Optional.of(user));

            assertThat(service.findUserByEmail(user.getEmail())).contains(user);
        }

        @Test
        void should_returnEmptyOptional_whenUserDoesntExist() {
            String email = "nouser@example.com";
            when(repository.findByEmailAndDeletedAtIsNull(email)).thenReturn(Optional.empty());

            assertThat(service.findUserByEmail(email)).isEmpty();
        }

        @Test
        void should_lookUpNormalizedEmail_whenEmailHasPaddingAndUpperCase() {
            User user = randomUser();
            when(repository.findByEmailAndDeletedAtIsNull("john.doe@example.com")).thenReturn(Optional.of(user));

            assertThat(service.findUserByEmail("  John.Doe@Example.COM ")).containsSame(user);
        }
    }

    @Nested
    class GetUserByEmail {
        @Test
        void should_returnUser_whenUserExistsAndIsActive() {
            User user = randomUser();
            when(repository.findByEmailAndDeletedAtIsNull(user.getEmail())).thenReturn(Optional.of(user));

            User result = service.getUserByEmail(user.getEmail());

            assertSame(user, result);
        }

        @Test
        void should_throwNotFound_whenUserDoesntExist() {
            String email = "nouser@example.com";
            when(repository.findByEmailAndDeletedAtIsNull(email)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getUserByEmail(email))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(email);
        }

        @Test
        void should_lookUpNormalizedEmail_whenEmailHasPaddingAndUpperCase() {
            User user = randomUser();
            when(repository.findByEmailAndDeletedAtIsNull("john.doe@example.com")).thenReturn(Optional.of(user));

            User result = service.getUserByEmail("  John.Doe@Example.COM ");

            assertSame(user, result);
        }

        @Test
        void should_reportNormalizedEmail_whenUserDoesntExist() {
            when(repository.findByEmailAndDeletedAtIsNull("john.doe@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getUserByEmail("  John.Doe@Example.COM "))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("john.doe@example.com");
        }
    }

    @Nested
    class GetAllUsers {
        @Test
        void should_returnUsersList_whenUsersExist() {
            List<User> userList = List.of(randomUser(), randomUser());
            when(repository.findAllByDeletedAtIsNullAndEmailVerifiedAtIsNotNull()).thenReturn(userList);

            List<User> result = service.getAllUsers();

            assertSame(userList, result);
        }

        @Test
        void should_returnEmptyList_whenUsersDontExist() {
            when(repository.findAllByDeletedAtIsNullAndEmailVerifiedAtIsNotNull()).thenReturn(List.of());
            List<User> result = service.getAllUsers();
            assertTrue(result.isEmpty());
        }

    }

    @Nested
    class PatchUser {

        @Test
        void should_returnPatchedUser_whenUserExists() {
            User user = randomUser();
            UserPatchCommand command = Instancio.create(UserPatchCommand.class);
            User persisted = randomUser();

            when(repository.findByIdAndDeletedAtIsNullAndEmailVerifiedAtIsNotNull(user.getId())).thenReturn(Optional.of(user));
            when(repository.save(any(User.class))).thenReturn(persisted);

            User result = service.patchUser(user.getId(), command);
            assertSame(persisted, result);

            verify(repository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertEquals(command.email(), saved.getEmail());
            assertEquals(command.firstName(), saved.getFirstName());
            assertEquals(command.lastName(), saved.getLastName());
        }

        @Test
        void should_notSave_whenUserDoesntExist() {
            UUID userId = UUID.randomUUID();
            UserPatchCommand command = Instancio.create(UserPatchCommand.class);
            when(repository.findByIdAndDeletedAtIsNullAndEmailVerifiedAtIsNotNull(userId))
                    .thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.patchUser(userId, command));

            verify(repository, never()).findByEmailAndDeletedAtIsNull(any());
            verify(repository, never()).save(any());
        }

        @Test
        void should_notSave_whenNewEmailAlreadyExists() {
            User user = randomUser();
            UserPatchCommand command = Instancio.create(UserPatchCommand.class);
            when(repository.findByIdAndDeletedAtIsNullAndEmailVerifiedAtIsNotNull(user.getId()))
                    .thenReturn(Optional.of(user));
            when(repository.findByEmailAndDeletedAtIsNull(command.email()))
                    .thenReturn(Optional.of(User.builder().email(command.email()).build()));

            assertThatThrownBy( () -> service.patchUser(user.getId(), command))
                    .isExactlyInstanceOf(ConflictException.class)
                    .hasMessageContaining("An account with this email already exists")
                    .extracting(e -> ((ConflictException) e).getField()).isEqualTo("email");


            verify(repository, never()).save(any());
        }

        @Test
        void should_throwConflict_whenNewEmailBecameUnavailableWhilePatching() {
            User user = randomUser();
            UserPatchCommand command = Instancio.create(UserPatchCommand.class);
            when(repository.findByIdAndDeletedAtIsNullAndEmailVerifiedAtIsNotNull(user.getId()))
                    .thenReturn(Optional.of(user));
            when(repository.findByEmailAndDeletedAtIsNull(command.email()))
                    .thenReturn(Optional.empty());
            when(repository.save(any(User.class))).thenThrow(MockDatabaseException.constraintViolation(UserConstraints.EMAIL_UNIQUE_CONSTRAINT));

            assertThatThrownBy( () -> service.patchUser(user.getId(), command))
                    .isExactlyInstanceOf(ConflictException.class)
                    .hasMessageContaining("An account with this email already exists")
                    .extracting(e -> ((ConflictException) e).getField()).isEqualTo("email");
        }

        @Test
        void should_notCheckEmail_whenEmailIsUnchanged() {
            User user = randomUser();
            UserPatchCommand command = Instancio.of(UserPatchCommand.class)
                    .set(field(UserPatchCommand::email), user.getEmail())
                    .create();
            User persisted = randomUser();

            when(repository.findByIdAndDeletedAtIsNullAndEmailVerifiedAtIsNotNull(user.getId())).thenReturn(Optional.of(user));
            when(repository.save(any(User.class))).thenReturn(persisted);

            User result = service.patchUser(user.getId(), command);
            assertSame(persisted, result);

            verify(repository, never()).findByEmailAndDeletedAtIsNull(any());
            verify(repository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertEquals(command.email(), saved.getEmail());
            assertEquals(command.firstName(), saved.getFirstName());
            assertEquals(command.lastName(), saved.getLastName());
        }

        @Test
        void should_keepEmail_whenEmailIsNotPatched() {
            User user = randomUser();
            String originalEmail = user.getEmail();
            UserPatchCommand command = Instancio.of(UserPatchCommand.class)
                    .set(field(UserPatchCommand::email), null)
                    .create();
            User persisted = randomUser();

            when(repository.findByIdAndDeletedAtIsNullAndEmailVerifiedAtIsNotNull(user.getId())).thenReturn(Optional.of(user));
            when(repository.save(any(User.class))).thenReturn(persisted);

            User result = service.patchUser(user.getId(), command);
            assertSame(persisted, result);

            verify(repository, never()).findByEmailAndDeletedAtIsNull(any());
            verify(repository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertEquals(originalEmail, saved.getEmail());
            assertEquals(command.firstName(), saved.getFirstName());
            assertEquals(command.lastName(), saved.getLastName());
        }

        @Test
        void should_keepOtherFields_whenOnlyFirstNameIsPatched() {
            User user = randomUser();
            String originalEmail = user.getEmail();
            String originalLastName = user.getLastName();
            UserPatchCommand command = Instancio.of(UserPatchCommand.class)
                    .set(field(UserPatchCommand::email), null)
                    .set(field(UserPatchCommand::lastName), null)
                    .create();
            User persisted = randomUser();

            when(repository.findByIdAndDeletedAtIsNullAndEmailVerifiedAtIsNotNull(user.getId())).thenReturn(Optional.of(user));
            when(repository.save(any(User.class))).thenReturn(persisted);

            User result = service.patchUser(user.getId(), command);
            assertSame(persisted, result);

            verify(repository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertEquals(command.firstName(), saved.getFirstName());
            assertEquals(originalLastName, saved.getLastName());
            assertEquals(originalEmail, saved.getEmail());
        }

        @Test
        void should_rethrowError_whenSaveFailsForAnotherReason() {
            User user = randomUser();
            UserPatchCommand command = Instancio.create(UserPatchCommand.class);
            DataIntegrityViolationException otherViolation = MockDatabaseException.otherViolation();
            when(repository.findByIdAndDeletedAtIsNullAndEmailVerifiedAtIsNotNull(user.getId()))
                    .thenReturn(Optional.of(user));
            when(repository.save(any(User.class))).thenThrow(otherViolation);

            assertThatThrownBy(() -> service.patchUser(user.getId(), command))
                    .isSameAs(otherViolation);
        }

    }

    @Nested
    class DeleteUser {

        @Test
        void should_markTheUserDeletedAndSave_whenUserExists() {
            User user = randomUser();

            when(repository.findByIdAndDeletedAtIsNullAndEmailVerifiedAtIsNotNull(user.getId()))
                    .thenReturn(Optional.of(user));

            service.deleteUserById(user.getId());

            verify(repository).save(userCaptor.capture());
            User saved = userCaptor.getValue();

            assertSame(user, saved);
            assertNotNull(saved.getDeletedAt());
        }

        @Test
        void should_notSave_whenUserDoesntExist() {
            UUID userId = UUID.randomUUID();
            when(repository.findByIdAndDeletedAtIsNullAndEmailVerifiedAtIsNotNull(userId))
                    .thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.deleteUserById(userId));

            verify(repository, never()).save(any());
        }
    }

    @Nested
    class VerifyUserEmail {
        @Test
        void should_saveUserWithEmailVerified() {
            User user = randomUserBuilder().set(field(User::getEmailVerifiedAt), null).create();

            service.verifyUserEmail(user);

            verify(repository).save(userCaptor.capture());
            assertSame(user, userCaptor.getValue());
            assertNotNull(userCaptor.getValue().getEmailVerifiedAt());
        }
    }

}
