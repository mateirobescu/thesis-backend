package com.mateirobescu.thesis.operations;

import com.mateirobescu.thesis.exception.InvalidPayloadException;
import com.mateirobescu.thesis.exception.NotFoundException;
import com.mateirobescu.thesis.projects.Project;
import com.mateirobescu.thesis.projects.ProjectService;
import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.users.UserService;
import org.instancio.Instancio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationServiceTest {

    @Mock
    private OperationRepository operationRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private UserService userService;

    @Mock
    private OperationPayloadParser payloadParser;

    @Captor
    private ArgumentCaptor<Operation> operationCaptor;

    /**
     * A real mapper: the stored payload is whatever it produces, so a mock would only return {@code null}.
     */
    @Spy
    private ObjectMapper objectMapper = JsonMapper.builder().build();

    @InjectMocks
    private OperationService service;

    private Project projectWithId(UUID id) {
        return Instancio.of(Project.class).set(field(Project::getId), id).create();
    }

    private Operation operation() {
        return Instancio.of(Operation.class)
                .set(field(Operation::getPayload), objectMapper.createObjectNode())
                .create();
    }

    @Nested
    class CreateOperation {

        private OperationCreateCommand command() {
            return Instancio.of(OperationCreateCommand.class)
                    .set(field(OperationCreateCommand::type), OperationType.FILE)
                    .set(field(OperationCreateCommand::payload), objectMapper.createObjectNode())
                    .create();
        }

        @Test
        void should_saveOperationWithParsedPayload_whenCommandIsValid() {
            OperationCreateCommand command = command();
            FileOperationPayload parsed = Instancio.create(FileOperationPayload.class);
            User user = Instancio.of(User.class).set(field(User::getId), command.userId()).create();
            Project project = projectWithId(command.projectId());
            Operation persisted = operation();
            when(payloadParser.parse(command.type(), command.payload())).thenReturn(parsed);
            when(userService.getUserById(command.userId())).thenReturn(user);
            when(projectService.getProjectWithNewSeq(command.projectId())).thenReturn(project);
            when(operationRepository.save(any(Operation.class))).thenReturn(persisted);

            Operation result = service.createOperation(command);
            assertSame(persisted, result);

            verify(operationRepository).save(operationCaptor.capture());
            Operation saved = operationCaptor.getValue();
            assertSame(project, saved.getProject());
            assertSame(user, saved.getUser());
            assertEquals(project.getSeq(), saved.getSeq());
            assertEquals(command.clientSeq(), saved.getClientSeq());
            assertEquals(command.clientTimestamp(), saved.getClientTimestamp());
            assertEquals(command.type(), saved.getType());
            assertEquals(objectMapper.valueToTree(parsed), saved.getPayload());
        }

        @Test
        void should_loadNothingAndSaveNothing_whenPayloadIsInvalid() {
            OperationCreateCommand command = command();
            when(payloadParser.parse(command.type(), command.payload()))
                    .thenThrow(new InvalidPayloadException("invalid", Map.of()));

            assertThrows(InvalidPayloadException.class, () -> service.createOperation(command));

            verifyNoInteractions(userService, projectService, operationRepository);
        }

        @Test
        void should_throwNotFound_whenUserDoesNotExist() {
            OperationCreateCommand command = command();
            when(payloadParser.parse(command.type(), command.payload()))
                    .thenReturn(Instancio.create(FileOperationPayload.class));
            when(userService.getUserById(command.userId())).thenThrow(new NotFoundException("User", command.userId()));

            assertThrows(NotFoundException.class, () -> service.createOperation(command));

            verifyNoInteractions(projectService);
            verify(operationRepository, never()).save(any());
        }

        @Test
        void should_throwNotFound_whenProjectDoesNotExist() {
            OperationCreateCommand command = command();
            User user = Instancio.of(User.class).set(field(User::getId), command.userId()).create();
            when(payloadParser.parse(command.type(), command.payload()))
                    .thenReturn(Instancio.create(FileOperationPayload.class));
            when(userService.getUserById(command.userId())).thenReturn(user);
            when(projectService.getProjectWithNewSeq(command.projectId()))
                    .thenThrow(new NotFoundException("Project", command.projectId()));

            assertThrows(NotFoundException.class, () -> service.createOperation(command));

            verify(operationRepository, never()).save(any());
        }
    }

    @Nested
    class GetOperationBySeq {

        @Test
        void should_returnOperation_whenItExists() {
            UUID projectId = UUID.randomUUID();
            Long seq = 7L;
            Project project = projectWithId(projectId);
            Operation operation = operation();
            when(projectService.getProjectById(projectId)).thenReturn(project);
            when(operationRepository.findByProjectAndSeq(project, seq)).thenReturn(Optional.of(operation));

            assertSame(operation, service.getOperationBySeq(projectId, seq));
        }

        @Test
        void should_throwNotFound_whenProjectDoesNotExist() {
            UUID projectId = UUID.randomUUID();
            when(projectService.getProjectById(projectId)).thenThrow(new NotFoundException("Project", projectId));

            assertThrows(NotFoundException.class, () -> service.getOperationBySeq(projectId, 7L));

            verifyNoInteractions(operationRepository);
        }

        @Test
        void should_throwNotFound_whenProjectHasNoOperationWithSeq() {
            UUID projectId = UUID.randomUUID();
            Long seq = 7L;
            Project project = projectWithId(projectId);
            when(projectService.getProjectById(projectId)).thenReturn(project);
            when(operationRepository.findByProjectAndSeq(project, seq)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.getOperationBySeq(projectId, seq));
        }
    }

    @Nested
    class GetProjectOperationsWithSeqGreaterThan {

        @Test
        void should_returnOperations_whenProjectExists() {
            UUID projectId = UUID.randomUUID();
            Long seq = 7L;
            Project project = projectWithId(projectId);
            List<Operation> operations = List.of(operation(), operation());
            when(projectService.getProjectById(projectId)).thenReturn(project);
            when(operationRepository.findByProjectAndSeqGreaterThanOrderBySeqAsc(project, seq)).thenReturn(operations);

            assertSame(operations, service.getProjectOperationsWithSeqGreaterThan(projectId, seq));
        }

        @Test
        void should_throwNotFound_whenProjectDoesNotExist() {
            UUID projectId = UUID.randomUUID();
            when(projectService.getProjectById(projectId)).thenThrow(new NotFoundException("Project", projectId));

            assertThrows(NotFoundException.class, () -> service.getProjectOperationsWithSeqGreaterThan(projectId, 7L));

            verifyNoInteractions(operationRepository);
        }
    }
}
