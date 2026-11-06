package com.mateirobescu.thesis.files;

import com.mateirobescu.thesis.exception.ConflictException;
import com.mateirobescu.thesis.exception.ConstraintViolations;
import com.mateirobescu.thesis.exception.NotFoundException;
import com.mateirobescu.thesis.projects.Project;
import com.mateirobescu.thesis.projects.ProjectCreateCommand;
import com.mateirobescu.thesis.projects.ProjectService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks, against a real PostgreSQL with the Flyway migrations applied, that the unique index on a file's path and
 * the version column behave the way {@code FileService} assumes. It lives in the {@code files} package because
 * {@code FileRepository} is package-private.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FileIntegrationTest {

    private static final String PATH = "src/Main.java";

    @Autowired
    private UserService userService;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private FileService fileService;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User owner;
    private Project project;

    @BeforeEach
    void setUp() {
        owner = userService.createUser(new UserCreateCommand("john.doe@example.com", "hash", "John", "Doe"));
        userService.verifyUserEmail(owner);
        Workspace workspace = workspaceService.createWorkspace(new WorkspaceCreateCommand("Doe Workspace", owner.getId()));
        project = projectService.createProject(new ProjectCreateCommand(workspace.getId(), "Doe Project", null, owner.getId()));
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
    }

    private File fileAt(Project project, String path) {
        return File.builder().project(project).owner(owner).path(path).build();
    }

    @Test
    void should_reportThePathConstraint_whenTwoActiveFilesShareAPath() {
        fileRepository.saveAndFlush(fileAt(project, PATH));

        DataIntegrityViolationException error = assertThrows(DataIntegrityViolationException.class,
                () -> fileRepository.saveAndFlush(fileAt(project, PATH)));

        assertTrue(ConstraintViolations.isViolationOf(error, FileConstraints.PROJECT_PATH_UNIQUE_CONSTRAINT));
    }

    @Test
    void should_throwConflict_whenFileIsCreatedAtAnExistingPath() {
        FileCreateCommand command = new FileCreateCommand(project.getId(), PATH, owner.getId());
        fileService.createFile(command);

        assertThrows(ConflictException.class, () -> fileService.createFile(command));
    }

    @Test
    void should_allowThePath_whenTheFileAtItIsSoftDeleted() {
        FileCreateCommand command = new FileCreateCommand(project.getId(), PATH, owner.getId());
        File first = fileService.createFile(command);
        fileService.deleteFile(project.getId(), first.getId());

        File second = assertDoesNotThrow(() -> fileService.createFile(command));

        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void should_allowTheSamePath_inDifferentProjects() {
        Project otherProject = projectService.createProject(
                new ProjectCreateCommand(project.getWorkspace().getId(), "Other Doe Project", null, owner.getId()));
        fileService.createFile(new FileCreateCommand(project.getId(), PATH, owner.getId()));

        assertDoesNotThrow(() -> fileService.createFile(new FileCreateCommand(otherProject.getId(), PATH, owner.getId())));
    }

    @Test
    void should_rejectTheStaleSave_whenFileIsSavedFromAnOldCopy() {
        File created = fileService.createFile(new FileCreateCommand(project.getId(), PATH, owner.getId()));
        File first = fileRepository.findById(created.getId()).orElseThrow();
        File second = fileRepository.findById(created.getId()).orElseThrow();
        fileRepository.save(first.markDeleted());

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> fileRepository.save(second.markDeleted()));
    }

    @Test
    void should_throwNotFound_whenFileByIdIsSoftDeleted() {
        File created = fileService.createFile(new FileCreateCommand(project.getId(), PATH, owner.getId()));
        fileService.deleteFile(project.getId(), created.getId());

        assertThrows(NotFoundException.class, () -> fileService.getFileById(project.getId(), created.getId()));
    }

    @Test
    void should_returnEmpty_whenFileFoundByPathIsSoftDeleted() {
        File created = fileService.createFile(new FileCreateCommand(project.getId(), PATH, owner.getId()));
        fileService.deleteFile(project.getId(), created.getId());

        assertTrue(fileService.findFileByPath(project.getId(), PATH).isEmpty());
    }

    @Test
    void should_excludeTheFile_fromTheProjectsFiles_whenSoftDeleted() {
        File deleted = fileService.createFile(new FileCreateCommand(project.getId(), PATH, owner.getId()));
        File kept = fileService.createFile(new FileCreateCommand(project.getId(), "src/Other.java", owner.getId()));
        fileService.deleteFile(project.getId(), deleted.getId());

        assertEquals(List.of(kept.getId()),
                fileService.getFilesByProject(project.getId()).stream().map(File::getId).toList());
    }

    @Test
    void should_throwNotFound_whenFileIsRequestedThroughAnotherProject() {
        Project otherProject = projectService.createProject(
                new ProjectCreateCommand(project.getWorkspace().getId(), "Other Doe Project", null, owner.getId()));
        File created = fileService.createFile(new FileCreateCommand(project.getId(), PATH, owner.getId()));

        assertThrows(NotFoundException.class, () -> fileService.getFileById(otherProject.getId(), created.getId()));
    }

    @Test
    void should_createOneFileAndConflictTheRest_whenManyCreatesRaceForTheSamePath() throws Exception {
        int threads = 10;
        FileCreateCommand command = new FileCreateCommand(project.getId(), PATH, owner.getId());

        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<File>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return fileService.createFile(command);
                }));
            }
            ready.await();
            start.countDown();

            int created = 0;
            for (Future<File> future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS);
                    created++;
                } catch (ExecutionException e) {
                    assertInstanceOf(ConflictException.class, e.getCause());
                }
            }

            assertEquals(1, created);
            assertEquals(1, fileService.getFilesByProject(project.getId()).size());
        }
    }
}
