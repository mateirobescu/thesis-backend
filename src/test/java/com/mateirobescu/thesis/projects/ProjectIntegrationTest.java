package com.mateirobescu.thesis.projects;

import com.mateirobescu.thesis.exception.ConflictException;
import com.mateirobescu.thesis.exception.ConstraintViolations;
import com.mateirobescu.thesis.exception.NotFoundException;
import com.mateirobescu.thesis.support.TestcontainersConfiguration;
import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.users.UserCreateCommand;
import com.mateirobescu.thesis.users.UserService;
import com.mateirobescu.thesis.workspaces.Workspace;
import com.mateirobescu.thesis.workspaces.WorkspaceCreateCommand;
import com.mateirobescu.thesis.workspaces.WorkspaceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks, against a real PostgreSQL with the Flyway migrations applied, that the unique index on a project's
 * (workspace, path, name) and the version column behave the way {@code ProjectService} assumes. It lives in the
 * {@code projects} package because {@code ProjectRepository} is package-private.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ProjectIntegrationTest {

    private static final String NAME = "Doe Project";

    @Autowired
    private UserService userService;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private User owner;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        owner = userService.createUser(new UserCreateCommand("john.doe@example.com", "hash", "John", "Doe"));
        userService.verifyUserEmail(owner);
        workspace = workspaceService.createWorkspace(new WorkspaceCreateCommand("Doe Workspace", owner.getId()));
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
    }

    private ProjectCreateCommand createCommand(Workspace workspace, String name, String path) {
        return new ProjectCreateCommand(workspace.getId(), name, path, owner.getId());
    }

    private Project projectAt(Workspace workspace, String name, String path) {
        return Project.builder().workspace(workspace).owner(owner).name(name).path(path).build();
    }

    @Test
    void should_reportTheLocationConstraint_whenTwoActiveProjectsShareALocation() {
        projectRepository.saveAndFlush(projectAt(workspace, NAME, "/"));

        DataIntegrityViolationException error = assertThrows(DataIntegrityViolationException.class,
                () -> projectRepository.saveAndFlush(projectAt(workspace, NAME, "/")));

        assertTrue(ConstraintViolations.isViolationOf(error, ProjectConstraints.WORKSPACE_PATH_NAME_UNIQUE_CONSTRAINT));
    }

    @Test
    void should_throwConflict_whenProjectIsCreatedAtAnExistingLocation() {
        projectService.createProject(createCommand(workspace, NAME, null));

        assertThrows(ConflictException.class,
                () -> projectService.createProject(createCommand(workspace, NAME, null)));
    }

    @Test
    void should_allowTheLocation_whenTheProjectAtItIsSoftDeleted() {
        Project first = projectService.createProject(createCommand(workspace, NAME, null));
        projectService.deleteProject(first.getId());

        Project second = assertDoesNotThrow(() -> projectService.createProject(createCommand(workspace, NAME, null)));

        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void should_allowTheSameName_inDifferentWorkspaces() {
        Workspace otherWorkspace = workspaceService.createWorkspace(new WorkspaceCreateCommand("Other Doe Workspace", owner.getId()));
        projectService.createProject(createCommand(workspace, NAME, null));

        assertDoesNotThrow(() -> projectService.createProject(createCommand(otherWorkspace, NAME, null)));
    }

    @Test
    void should_allowTheSameName_atDifferentPaths() {
        projectService.createProject(createCommand(workspace, NAME, "/"));

        assertDoesNotThrow(() -> projectService.createProject(createCommand(workspace, NAME, "/archive")));
    }

    @Test
    void should_throwConflict_whenProjectIsPatchedIntoAnOccupiedLocation() {
        projectService.createProject(createCommand(workspace, NAME, null));
        Project other = projectService.createProject(createCommand(workspace, "Other Doe Project", null));

        assertThrows(ConflictException.class,
                () -> projectService.patchProject(other.getId(), new ProjectPatchCommand(null, NAME, null, null)));
    }

    @Test
    void should_rejectTheStaleSave_whenProjectIsSavedFromAnOldCopy() {
        Project created = projectService.createProject(createCommand(workspace, NAME, null));
        Project first = projectRepository.findById(created.getId()).orElseThrow();
        Project second = projectRepository.findById(created.getId()).orElseThrow();
        projectRepository.save(first.markDeleted());

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> projectRepository.save(second.markDeleted()));
    }

    @Test
    void should_throwNotFound_whenProjectByIdIsSoftDeleted() {
        Project created = projectService.createProject(createCommand(workspace, NAME, null));
        projectService.deleteProject(created.getId());

        assertThrows(NotFoundException.class, () -> projectService.getProjectById(created.getId()));
    }

    @Test
    void should_excludeTheProject_fromTheWorkspacesProjects_whenSoftDeleted() {
        Project deleted = projectService.createProject(createCommand(workspace, NAME, null));
        Project kept = projectService.createProject(createCommand(workspace, "Other Doe Project", null));
        projectService.deleteProject(deleted.getId());

        assertEquals(List.of(kept.getId()),
                projectService.getProjectsByWorkspace(workspace.getId()).stream().map(Project::getId).toList());
    }

    @Test
    void should_throwNotFound_whenSeqIsRequestedForASoftDeletedProject() {
        Project created = projectService.createProject(createCommand(workspace, NAME, null));
        projectService.deleteProject(created.getId());

        assertThrows(NotFoundException.class,
                () -> transactionTemplate.execute(status -> projectService.getProjectWithNewSeq(created.getId())));
    }

    @Test
    void should_createOneProjectAndConflictTheRest_whenManyCreatesRaceForTheSameLocation() throws Exception {
        int threads = 10;
        ProjectCreateCommand command = createCommand(workspace, NAME, "/");
        try(ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Project>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return projectService.createProject(command);
                }));
            }

            ready.await();
            start.countDown();

            int created = 0;
            for(var future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS);
                    created++;
                } catch (ExecutionException e) {
                    assertInstanceOf(ConflictException.class, e.getCause());
                }
            }

            assertEquals(1, created);
            assertEquals(1, projectService.getProjectsByWorkspace(workspace.getId()).size());
        }

    }
}
