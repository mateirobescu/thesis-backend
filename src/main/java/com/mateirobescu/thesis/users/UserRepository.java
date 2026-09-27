package com.mateirobescu.thesis.users;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByIdAndDeletedAtIsNull(UUID id);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);
}
