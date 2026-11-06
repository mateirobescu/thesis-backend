package com.mateirobescu.thesis.users;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface UserRepository extends JpaRepository<User, UUID> {
    /**
     * Finds an active (not soft-deleted) user whose email is verified.
     */
    Optional<User> findByIdAndDeletedAtIsNullAndEmailVerifiedAtIsNotNull(UUID id);

    /**
     * Finds a user that is not soft-deleted by exact email, verified or not.
     */
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    //TODO this doesn t throw errors for retrying the same email adrress when not verified
//    Boolean existsByEmailAndDeletedAtIsNullAndEmailVerifiedAtIsNotNull(String email);
    Boolean existsByEmailAndDeletedAtIsNull(String email);

    /**
     * Lists all active users whose email is verified.
     */
    List<User> findAllByDeletedAtIsNullAndEmailVerifiedAtIsNotNull();
}
