package com.mateirobescu.thesis.exception;

import com.mateirobescu.thesis.support.MockDatabaseException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstraintViolationsTest {

    private static final String CONSTRAINT = "uk_users_email_active";

    private ConstraintViolationException violationOf(String constraintName) {
        return new ConstraintViolationException("violation", new SQLException(), constraintName);
    }

    @Nested
    class IsViolationOf {

        @Test
        void should_returnTrue_whenErrorIsTheViolationItself() {
            assertTrue(ConstraintViolations.isViolationOf(violationOf(CONSTRAINT), CONSTRAINT));
        }

        @Test
        void should_returnTrue_whenViolationIsWrappedOnce() {
            assertTrue(ConstraintViolations.isViolationOf(MockDatabaseException.constraintViolation(CONSTRAINT), CONSTRAINT));
        }

        @Test
        void should_returnTrue_whenViolationIsDeepInTheCauseChain() {
            Throwable error = new RuntimeException("outer", MockDatabaseException.constraintViolation(CONSTRAINT));

            assertTrue(ConstraintViolations.isViolationOf(error, CONSTRAINT));
        }

        @Test
        void should_returnTrue_whenNamesDifferOnlyByCase() {
            assertTrue(ConstraintViolations.isViolationOf(
                    MockDatabaseException.constraintViolation(CONSTRAINT.toUpperCase()), CONSTRAINT));
        }

        @Test
        void should_returnFalse_whenConstraintNameIsDifferent() {
            assertFalse(ConstraintViolations.isViolationOf(
                    MockDatabaseException.constraintViolation("uk_files_project_path_active"), CONSTRAINT));
        }

        @Test
        void should_returnFalse_whenConstraintNameOnlySharesAPrefix() {
            assertFalse(ConstraintViolations.isViolationOf(
                    MockDatabaseException.constraintViolation(CONSTRAINT + "_old"), CONSTRAINT));
        }

        @Test
        void should_returnFalse_whenChainHasNoConstraintViolation() {
            assertFalse(ConstraintViolations.isViolationOf(MockDatabaseException.otherViolation(), CONSTRAINT));
        }

        @Test
        void should_returnFalse_whenViolationHasNoConstraintName() {
            assertFalse(ConstraintViolations.isViolationOf(violationOf(null), CONSTRAINT));
        }

        @Test
        void should_returnFalse_whenErrorIsNull() {
            assertFalse(ConstraintViolations.isViolationOf(null, CONSTRAINT));
        }
    }
}
