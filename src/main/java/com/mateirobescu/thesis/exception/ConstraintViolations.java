package com.mateirobescu.thesis.exception;

import org.hibernate.exception.ConstraintViolationException;

//TODO maybe take a better look at this
/**
 * Helper for telling which database constraint an error came from, so that a service can translate a violation of
 * one specific constraint and rethrow everything else.
 */
public final class ConstraintViolations {

    private ConstraintViolations() {
    }

    /**
     * Tells whether the error, or any of its causes, is a Hibernate {@link ConstraintViolationException} for the
     * given constraint. The name must match in full, ignoring case.
     *
     * @param error          the error to inspect, usually the {@code DataIntegrityViolationException} a repository threw
     * @param constraintName the database name of the constraint, e.g. {@code uk_users_email_active}
     * @return {@code true} if the error was caused by a violation of that constraint
     */
    public static boolean isViolationOf(Throwable error, String constraintName) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && constraintName.equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
