package com.mateirobescu.thesis.users;

import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UserCreateCommandTest {

    @Test
    void should_trimAndLowercaseEmail() {
        UserCreateCommand command = Instancio.of(UserCreateCommand.class)
                .set(field(UserCreateCommand::email), "  John@Example.COM ")
                .create();

        assertEquals("john@example.com", command.email());
    }

    @Test
    void should_trimNames() {
        UserCreateCommand command = Instancio.of(UserCreateCommand.class)
                .set(field(UserCreateCommand::firstName), "  John ")
                .set(field(UserCreateCommand::lastName), " Doe  ")
                .create();

        assertEquals("John", command.firstName());
        assertEquals("Doe", command.lastName());
    }
}
