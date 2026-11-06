package com.mateirobescu.thesis.workspaces;

import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WorkspacePatchCommandTest {

    @Test
    void should_trimName_whenPresent() {
        WorkspacePatchCommand command = Instancio.of(WorkspacePatchCommand.class)
                .set(field(WorkspacePatchCommand::name), "  Jane Doe Workspace ")
                .create();

        assertEquals("Jane Doe Workspace", command.name());
    }

    @Test
    void should_keepFieldsNull_whenNotProvided() {
        WorkspacePatchCommand command = new WorkspacePatchCommand(null, null);

        assertNull(command.name());
        assertNull(command.ownerId());
    }

    @Test
    void should_keepOwnerId_whenNameIsNotProvided() {
        UUID ownerId = UUID.randomUUID();

        WorkspacePatchCommand command = new WorkspacePatchCommand(null, ownerId);

        assertNull(command.name());
        assertEquals(ownerId, command.ownerId());
    }
}
