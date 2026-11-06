package com.mateirobescu.thesis.support;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

/**
 * Factory for the exceptions a repository throws when the database rejects a write, so that service tests
 * do not have to build the Spring and Hibernate exception chain by hand.
 */
public final class MockDatabaseException {

    private MockDatabaseException() {
    }

    /**
     * Builds the exception thrown when a write violates the given constraint: a
     * {@link DataIntegrityViolationException} caused by a Hibernate {@link ConstraintViolationException}.
     *
     * @param constraintName the database name of the violated constraint, e.g. {@code uk_users_email_active}
     * @return the exception to pass to {@code thenThrow}
     */
    public static DataIntegrityViolationException constraintViolation(String constraintName) {
        ConstraintViolationException cause =
                new ConstraintViolationException("violated " + constraintName, new SQLException(), constraintName);
        return new DataIntegrityViolationException("violated " + constraintName, cause);
    }

    /**
     * Builds an integrity error that is not tied to any constraint, for checking that a service rethrows
     * errors it does not know how to translate.
     *
     * @return the exception to pass to {@code thenThrow}
     */
    public static DataIntegrityViolationException otherViolation() {
        return new DataIntegrityViolationException("other violation");
    }
}
