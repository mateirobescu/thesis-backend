package com.mateirobescu.thesis.workspaces;

import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkspaceCreateCommandTest {

    @Test
    void should_trimName() {
        WorkspaceCreateCommand command = Instancio.of(WorkspaceCreateCommand.class)
                .set(field(WorkspaceCreateCommand::name), "  John Doe Workspace ")
                .create();

        assertEquals("John Doe Workspace", command.name());
    }
}
