package com.mateirobescu.thesis.validation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidPathValidatorTest {

    private final ValidPathValidator validator = new ValidPathValidator();

    @Nested
    class IsValid {

        @Test
        void should_beValid_whenPathIsNull() {
            assertTrue(validator.isValid(null, null));
        }

        @Test
        void should_beInvalid_whenPathIsEmpty() {
            assertFalse(validator.isValid("", null));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Main.java",
                "src/Main.java",
                "src/main/java/App.java",
                "dir with space/file name.txt",
                ".gitignore",
                "a/b.c/d"
        })
        void should_beValid_whenPathIsRelativeAndNormalized(String path) {
            assertTrue(validator.isValid(path, null));
        }

        @ParameterizedTest
        @ValueSource(strings = {"src\\Main.java", "\\src", "src\\"})
        void should_beInvalid_whenPathContainsBackslash(String path) {
            assertFalse(validator.isValid(path, null));
        }

        @ParameterizedTest
        @ValueSource(strings = {"/src/Main.java", "/"})
        void should_beInvalid_whenPathStartsWithSlash(String path) {
            assertFalse(validator.isValid(path, null));
        }

        @ParameterizedTest
        @ValueSource(strings = {"src/", "src/main/"})
        void should_beInvalid_whenPathEndsWithSlash(String path) {
            assertFalse(validator.isValid(path, null));
        }

        @ParameterizedTest
        @ValueSource(strings = {"src//Main.java", "a//b//c"})
        void should_beInvalid_whenPathContainsDoubleSlash(String path) {
            assertFalse(validator.isValid(path, null));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "./Main.java",
                "src/./Main.java",
                "src/../Main.java",
                ".",
                "src/..",
                "../Main.java",
                "src/../../../../Main.js"
        })
        void should_beInvalid_whenPathIsNotNormalized(String path) {
            assertFalse(validator.isValid(path, null));
        }

        @Test
        void should_beInvalid_whenPathContainsNulCharacter() {
            assertFalse(validator.isValid("src/Ma\u0000in.java", null));
        }
    }
}
