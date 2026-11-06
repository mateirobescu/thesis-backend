package com.mateirobescu.thesis.users;

/**
 * Names of the constraints on the {@code users} table that the code translates into API errors.
 */
public final class UserConstraints {

    private UserConstraints() {
    }

    /**
     * Partial unique index on {@code lower(email)} among users that are not soft-deleted, defined in the Flyway migration.
     */
    public static final String EMAIL_UNIQUE_CONSTRAINT = "uk_users_email_active";
}
