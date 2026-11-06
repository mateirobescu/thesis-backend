package com.mateirobescu.thesis.operations;

import com.mateirobescu.thesis.exception.InvalidPayloadException;
import com.mateirobescu.thesis.exception.NotFoundException;
import com.mateirobescu.thesis.projects.Project;
import com.mateirobescu.thesis.users.User;
import org.instancio.Instancio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks the HTTP layer of {@code OperationController}: routes, status codes and JSON, with the real exception
 * handler. The security filters are switched off and the service is mocked, so nothing here touches a database, a
 * token or the payload parser. Creating an operation reads the current user from the security context, so the tests
 * put one there.
 */
@WebMvcTest(OperationController.class)
@AutoConfigureMockMvc(addFilters = false)
class OperationControllerTest {

    private static final UUID CURRENT_USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OperationService operationService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CURRENT_USER_ID.toString(), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Operation operation(UUID projectId, UUID userId, long seq, FileOperationPayload payload) {
        Project project = Instancio.of(Project.class).set(field(Project::getId), projectId).create();
        User user = Instancio.of(User.class).set(field(User::getId), userId).create();
        return Instancio.of(Operation.class)
                .set(field(Operation::getProject), project)
                .set(field(Operation::getUser), user)
                .set(field(Operation::getSeq), seq)
                .set(field(Operation::getType), OperationType.FILE)
                .set(field(Operation::getPayload), objectMapper.valueToTree(payload))
                .create();
    }

    private OperationCreateRequest request(FileOperationPayload payload) {
        return new OperationCreateRequest(
                7L, Instant.now().truncatedTo(ChronoUnit.MILLIS), OperationType.FILE, objectMapper.valueToTree(payload));
    }

    @Nested
    class CreateOperation {

        private String body(Object request) {
            return objectMapper.writeValueAsString(request);
        }

        @Test
        void should_createTheOperationForTheCurrentUserInThePathProject_whenRequestIsValid() throws Exception {
            UUID projectId = UUID.randomUUID();
            FileOperationPayload payload = Instancio.create(FileOperationPayload.class);
            OperationCreateRequest request = request(payload);
            Operation created = operation(projectId, CURRENT_USER_ID, 3, payload);
            when(operationService.createOperation(any())).thenReturn(created);

            mockMvc.perform(post("/projects/{projectId}/operations", projectId)
                            .contentType(MediaType.APPLICATION_JSON).content(body(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(created.getId().toString()))
                    .andExpect(jsonPath("$.userId").value(CURRENT_USER_ID.toString()))
                    .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                    .andExpect(jsonPath("$.seq").value(3))
                    .andExpect(jsonPath("$.type").value("FILE"))
                    .andExpect(jsonPath("$.payload.fileId").value(payload.fileId().toString()))
                    .andExpect(jsonPath("$.envelope").doesNotExist());

            ArgumentCaptor<OperationCreateCommand> commandCaptor = ArgumentCaptor.forClass(OperationCreateCommand.class);
            verify(operationService).createOperation(commandCaptor.capture());
            OperationCreateCommand command = commandCaptor.getValue();
            assertEquals(CURRENT_USER_ID, command.userId());
            assertEquals(projectId, command.projectId());
            assertEquals(request.clientSeq(), command.clientSeq());
            assertEquals(request.clientTimestamp(), command.clientTimestamp());
            assertEquals(OperationType.FILE, command.type());
            assertEquals(payload, objectMapper.treeToValue(command.payload(), FileOperationPayload.class));
        }

        @Test
        void should_return400WithFieldErrors_whenRequiredFieldsAreMissing() throws Exception {
            mockMvc.perform(post("/projects/{projectId}/operations", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON).content(body(objectMapper.createObjectNode())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors.clientSeq").isNotEmpty())
                    .andExpect(jsonPath("$.errors.clientTimestamp").isNotEmpty())
                    .andExpect(jsonPath("$.errors.type").isNotEmpty())
                    .andExpect(jsonPath("$.errors.payload").isNotEmpty());

            verifyNoInteractions(operationService);
        }

        @Test
        void should_return400_whenTheTypeIsUnknown() throws Exception {
            ObjectNode request = objectMapper.valueToTree(request(Instancio.create(FileOperationPayload.class)));
            request.put("type", "BANANA");

            mockMvc.perform(post("/projects/{projectId}/operations", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON).content(body(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST_BODY"));

            verifyNoInteractions(operationService);
        }

        @Test
        void should_return400WithPayloadErrors_whenThePayloadDoesNotMatchTheType() throws Exception {
            when(operationService.createOperation(any())).thenThrow(
                    new InvalidPayloadException("Payload is not a valid FILE operation",
                            Map.of("payload.chars", List.of("must not be null"))));

            mockMvc.perform(post("/projects/{projectId}/operations", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(request(Instancio.create(FileOperationPayload.class)))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors['payload.chars'][0]").value("must not be null"));
        }

        @Test
        void should_return404_whenTheProjectDoesNotExist() throws Exception {
            UUID projectId = UUID.randomUUID();
            when(operationService.createOperation(any())).thenThrow(new NotFoundException("Project", projectId));

            mockMvc.perform(post("/projects/{projectId}/operations", projectId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(request(Instancio.create(FileOperationPayload.class)))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        void should_return400_whenProjectIdIsNotAUuid() throws Exception {
            mockMvc.perform(post("/projects/not-a-uuid/operations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(request(Instancio.create(FileOperationPayload.class)))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"));

            verifyNoInteractions(operationService);
        }
    }

    @Nested
    class GetOperationBySeq {

        @Test
        void should_returnTheOperation_whenItExists() throws Exception {
            UUID projectId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            FileOperationPayload payload = Instancio.create(FileOperationPayload.class);
            Operation found = operation(projectId, userId, 7, payload);
            when(operationService.getOperationBySeq(projectId, 7L)).thenReturn(found);

            mockMvc.perform(get("/projects/{projectId}/operations/{seq}", projectId, 7))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(found.getId().toString()))
                    .andExpect(jsonPath("$.userId").value(userId.toString()))
                    .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                    .andExpect(jsonPath("$.seq").value(7))
                    .andExpect(jsonPath("$.type").value("FILE"))
                    .andExpect(jsonPath("$.payload.fileId").value(payload.fileId().toString()));
        }

        @Test
        void should_return404_whenThereIsNoOperationWithTheSeq() throws Exception {
            UUID projectId = UUID.randomUUID();
            when(operationService.getOperationBySeq(projectId, 7L)).thenThrow(new NotFoundException("Operation", 7L));

            mockMvc.perform(get("/projects/{projectId}/operations/{seq}", projectId, 7))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        void should_return400_whenSeqIsNotANumber() throws Exception {
            mockMvc.perform(get("/projects/{projectId}/operations/not-a-number", UUID.randomUUID()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"));

            verifyNoInteractions(operationService);
        }
    }

    @Nested
    class GetOperationsWithSeqGreaterThan {

        @Test
        void should_returnTheOperationsInOrder_whenListing() throws Exception {
            UUID projectId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            when(operationService.getProjectOperationsWithSeqGreaterThan(projectId, 4L)).thenReturn(List.of(
                    operation(projectId, userId, 5, Instancio.create(FileOperationPayload.class)),
                    operation(projectId, userId, 6, Instancio.create(FileOperationPayload.class))));

            mockMvc.perform(get("/projects/{projectId}/operations", projectId).param("seqGreaterThan", "4"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.operations.length()").value(2))
                    .andExpect(jsonPath("$.operations[0].seq").value(5))
                    .andExpect(jsonPath("$.operations[1].seq").value(6))
                    .andExpect(jsonPath("$.operations[0].type").value("FILE"));
        }

        @Test
        void should_returnAnEmptyList_whenThereAreNoNewOperations() throws Exception {
            UUID projectId = UUID.randomUUID();
            when(operationService.getProjectOperationsWithSeqGreaterThan(projectId, 9L)).thenReturn(List.of());

            mockMvc.perform(get("/projects/{projectId}/operations", projectId).param("seqGreaterThan", "9"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.operations.length()").value(0));
        }

        @Test
        void should_return400_whenSeqGreaterThanIsMissing() throws Exception {
            mockMvc.perform(get("/projects/{projectId}/operations", UUID.randomUUID()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

            verifyNoInteractions(operationService);
        }

        @Test
        void should_return400_whenSeqGreaterThanIsNotANumber() throws Exception {
            mockMvc.perform(get("/projects/{projectId}/operations", UUID.randomUUID()).param("seqGreaterThan", "abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"));

            verifyNoInteractions(operationService);
        }

        @Test
        void should_return404_whenTheProjectDoesNotExist() throws Exception {
            UUID projectId = UUID.randomUUID();
            when(operationService.getProjectOperationsWithSeqGreaterThan(projectId, 0L))
                    .thenThrow(new NotFoundException("Project", projectId));

            mockMvc.perform(get("/projects/{projectId}/operations", projectId).param("seqGreaterThan", "0"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }
    }
}
