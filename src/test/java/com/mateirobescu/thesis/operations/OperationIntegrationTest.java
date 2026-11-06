package com.mateirobescu.thesis.operations;

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
import org.instancio.Instancio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks, against a real PostgreSQL with the Flyway migrations applied, that operations created at the same time
 * get distinct, consecutive sequence numbers, and that the lookup by sequence number returns the right operations
 * in the right order. It lives in the {@code operations} package because {@code OperationRepository} is
 * package-private.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OperationIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private OperationService operationService;

    @Autowired
    private OperationRepository operationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User owner;
    private Workspace workspace;
    private Project project;
    private long nextClientSeq = 0;

    @BeforeEach
    void setUp() {
        owner = userService.createUser(new UserCreateCommand("john.doe@example.com", "hash", "John", "Doe"));
        userService.verifyUserEmail(owner);
        workspace = workspaceService.createWorkspace(new WorkspaceCreateCommand("Doe Workspace", owner.getId()));
        project = projectService.createProject(new ProjectCreateCommand(workspace.getId(), "Doe Project", null, owner.getId()));
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
    }

    private Operation createOperation(Project project, FileOperationPayload payload) {
        return operationService.createOperation(new OperationCreateCommand(
                owner.getId(), project.getId(), nextClientSeq++, Instant.now(), OperationType.FILE,
                objectMapper.valueToTree(payload)));
    }

    private Operation createOperation(Project project) {
        return createOperation(project, Instancio.create(FileOperationPayload.class));
    }

    private List<Long> seqsOf(List<Operation> operations) {
        return operations.stream().map(Operation::getSeq).toList();
    }

    @Test
    void should_assignDistinctConsecutiveSeqs_whenOperationsAreCreatedConcurrently() throws Exception {
        final int threads = 10;
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Operation>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return createOperation(project);
                }));
            }

            ready.await();
            start.countDown();

            List<Long> seqs = new ArrayList<>();
            for (Future<Operation> future : futures) {
                seqs.add(future.get(30, TimeUnit.SECONDS).getSeq());
            }
            Collections.sort(seqs);

            assertEquals(threads, new HashSet<>(seqs).size());
            for (int i = 0; i < threads; i++) {
                assertEquals(seqs.getFirst() + i, seqs.get(i));
            }
            assertEquals(threads, operationRepository.count());
        }
    }

    @Test
    void should_returnOnlyOperationsAfterTheSeq_whenSeqIsGiven() {
        Operation first = createOperation(project);
        Operation second = createOperation(project);
        Operation third = createOperation(project);

        List<Operation> result = operationService.getProjectOperationsWithSeqGreaterThan(project.getId(), first.getSeq());

        assertEquals(List.of(second.getSeq(), third.getSeq()), seqsOf(result));
    }

    @Test
    void should_returnOperationsOldestFirst_whenSeqIsBeforeTheFirstOperation() {
        Operation first = createOperation(project);
        Operation second = createOperation(project);
        Operation third = createOperation(project);

        List<Operation> result = operationService.getProjectOperationsWithSeqGreaterThan(project.getId(), 0L);

        assertEquals(List.of(first.getSeq(), second.getSeq(), third.getSeq()), seqsOf(result));
    }

    @Test
    void should_notReturnOperationsOfOtherProjects_whenListingAProject() {
        Project otherProject = projectService.createProject(
                new ProjectCreateCommand(workspace.getId(), "Other Doe Project", null, owner.getId()));
        Operation mine = createOperation(project);
        createOperation(otherProject);

        List<Operation> result = operationService.getProjectOperationsWithSeqGreaterThan(project.getId(), 0L);

        assertEquals(List.of(mine.getSeq()), seqsOf(result));
    }

    @Test
    void should_returnThePayloadIntact_whenOperationIsReadBack() throws Exception {
        FileOperationPayload payload = Instancio.create(FileOperationPayload.class);
        createOperation(project, payload);

        List<Operation> result = operationService.getProjectOperationsWithSeqGreaterThan(project.getId(), 0L);

        assertEquals(payload, objectMapper.treeToValue(result.getFirst().getPayload(), FileOperationPayload.class));
    }
}
