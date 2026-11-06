package com.mateirobescu.thesis.users;

import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserPatchCommandTest {

    @Test
    void should_trimAndLowercaseEmail_whenPresent() {
        UserPatchCommand command = Instancio.of(UserPatchCommand.class)
                .set(field(UserPatchCommand::email), "  John@Example.COM ")
                .create();

        assertEquals("john@example.com", command.email());
    }

    @Test
    void should_trimNames_whenPresent() {
        UserPatchCommand command = Instancio.of(UserPatchCommand.class)
                .set(field(UserPatchCommand::firstName), "  John ")
                .set(field(UserPatchCommand::lastName), " Doe  ")
                .create();

        assertEquals("John", command.firstName());
        assertEquals("Doe", command.lastName());
    }

    @Test
    void should_keepFieldsNull_whenNotProvided() {
        UserPatchCommand command = new UserPatchCommand(null, null, null);

        assertNull(command.email());
        assertNull(command.firstName());
        assertNull(command.lastName());
    }
}
