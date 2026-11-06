package com.mateirobescu.thesis.workspaces;

import com.mateirobescu.thesis.exception.NotFoundException;
import com.mateirobescu.thesis.support.TestcontainersConfiguration;
import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.users.UserCreateCommand;
import com.mateirobescu.thesis.users.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks, against a real PostgreSQL with the Flyway migrations applied, that the version column of a workspace
 * stops a save made from a stale copy. It lives in the {@code workspaces} package because
 * {@code WorkspaceRepository} is package-private.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class WorkspaceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = userService.createUser(new UserCreateCommand("john.doe@example.com", "hash", "John", "Doe"));
        userService.verifyUserEmail(owner);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
    }

    @Test
    void should_rejectTheStaleSave_whenWorkspaceIsSavedFromAnOldCopy() {
        Workspace created = workspaceService.createWorkspace(new WorkspaceCreateCommand("Doe Workspace", owner.getId()));
        Workspace first = workspaceRepository.findById(created.getId()).orElseThrow();
        Workspace second = workspaceRepository.findById(created.getId()).orElseThrow();
        workspaceRepository.save(first.markDeleted());

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> workspaceRepository.save(second.markDeleted()));
    }

    @Test
    void should_throwNotFound_whenWorkspaceByIdIsSoftDeleted() {
        Workspace created = workspaceService.createWorkspace(new WorkspaceCreateCommand("Doe Workspace", owner.getId()));
        workspaceService.deleteWorkspace(created.getId());

        assertThrows(NotFoundException.class, () -> workspaceService.getWorkspaceById(created.getId()));
    }

    @Test
    void should_excludeTheWorkspace_fromTheOwnersWorkspaces_whenSoftDeleted() {
        Workspace deleted = workspaceService.createWorkspace(new WorkspaceCreateCommand("Doe Workspace", owner.getId()));
        Workspace kept = workspaceService.createWorkspace(new WorkspaceCreateCommand("Other Doe Workspace", owner.getId()));
        workspaceService.deleteWorkspace(deleted.getId());

        assertEquals(List.of(kept.getId()),
                workspaceService.getWorkspacesByUser(owner.getId()).stream().map(Workspace::getId).toList());
    }
}
