package com.mateirobescu.thesis.users;

import com.mateirobescu.thesis.exception.ConflictException;
import com.mateirobescu.thesis.exception.ConstraintViolations;
import com.mateirobescu.thesis.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * User accounts: creation, lookup, patching, soft deletion and email verification.
 * Emails are always compared in normalized form (trimmed and lower-cased).
 */
@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Creates a new user, who is not yet email-verified.
     *
     * @param command the sign-up data, already normalized by the command
     * @return the saved user
     * @throws ConflictException with field {@code email} if an active account already uses the email, whether
     *                           found by the pre-check or reported by the unique index when two sign-ups race
     */
    public User createUser(UserCreateCommand command) {
        //TODO an unverified account still holds its email, so its real owner cannot sign up again; resend the verification or let a new sign-up replace the unverified account
        if(this.findUserByEmail(command.email()).isPresent()) {
            throw new ConflictException("email", "An account with this email already exists");
        }

        User newUser = User.builder()
                .email(command.email())
                .passwordHash(command.passwordHash())
                .firstName(command.firstName())
                .lastName(command.lastName())
                .build();
        try {
            //TODO look into saveAndFlush
            User savedUser = userRepository.save(newUser);
            log.info("User created id={}", savedUser.getId());
            return savedUser;
        } catch (DataIntegrityViolationException error) {
            if(ConstraintViolations.isViolationOf(error, UserConstraints.EMAIL_UNIQUE_CONSTRAINT)) {
                log.info("Sign-up rejected, email already in use (constraint={})", UserConstraints.EMAIL_UNIQUE_CONSTRAINT);
                //TODO should switch to error codes for internationalisation
                throw new ConflictException("email", "An account with this email already exists");
            }
            throw error;
        }
    }

    /**
     * Returns a user who is active (not soft-deleted) and has verified their email.
     *
     * @throws NotFoundException if there is no such user, including when the user is soft-deleted or not yet verified
     */
    public User getUserById(UUID id) {
        return userRepository.findByIdAndDeletedAtIsNullAndEmailVerifiedAtIsNotNull(id).orElseThrow(() -> new NotFoundException("User", id));
    }

    /**
     * Looks up a user that is not soft-deleted by email, verified or not. The email is trimmed and lower-cased first.
     *
     * @return the user, or empty if there is none
     */
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Same lookup as {@link #findUserByEmail(String)}, but for callers that require the user to exist.
     *
     * @throws NotFoundException if there is no user with that email that is not soft-deleted
     */
    public User getUserByEmail(String email) {
        return this.findUserByEmail(email).orElseThrow(() -> new NotFoundException("User", email.trim().toLowerCase(Locale.ROOT)));
    }

    /**
     * Returns every active, email-verified user.
     */
    public List<User> getAllUsers() {
        return userRepository.findAllByDeletedAtIsNullAndEmailVerifiedAtIsNotNull();
    }

    /**
     * Changes the fields present in the command on an active, verified user; {@code null} fields are left as they are.
     *
     * @return the saved user
     * @throws NotFoundException if the user does not exist
     * @throws ConflictException with field {@code email} if the new email belongs to another account. The
     *                           uniqueness check is skipped when the command has no email or it is unchanged.
     */
    public User patchUser(UUID userId, UserPatchCommand command) {
        User userToPatch = this.getUserById(userId);
        if (command.email() != null
                && !command.email().equalsIgnoreCase(userToPatch.getEmail())
                && this.findUserByEmail(command.email()).isPresent()) {
            throw new ConflictException("email", "An account with this email already exists");
        }

        try {
            //TODO look into save and flush
            User patchedUser = userRepository.save(userToPatch.patch(command));
            log.info("User patched id={}", userToPatch.getId());
            return patchedUser;
        } catch (DataIntegrityViolationException error) {
            if (ConstraintViolations.isViolationOf(error, UserConstraints.EMAIL_UNIQUE_CONSTRAINT)) {
                throw new ConflictException("email", "An account with this email already exists");
            }
            throw error;
        }
    }

    /**
     * Soft-deletes an active, verified user by setting {@code deletedAt}; the row is kept.
     *
     * @throws NotFoundException if the user does not exist
     */
    public void deleteUserById(UUID id) {
        User userToDelete = this.getUserById(id);
        userToDelete.markDeleted();
        userRepository.save(userToDelete);
        log.info("User marked deleted id={}", id);
    }


    /**
     * Marks the user's email as verified and saves the user.
     */
    public void verifyUserEmail(User user) {
        userRepository.save(user.markEmailVerified());
    }
}
