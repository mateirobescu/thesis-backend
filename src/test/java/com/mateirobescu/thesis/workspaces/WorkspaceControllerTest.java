package com.mateirobescu.thesis.workspaces;

import com.mateirobescu.thesis.exception.NotFoundException;
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

import java.util.List;
import java.util.UUID;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks the HTTP layer of {@code WorkspaceController}: routes, status codes and JSON, with the real exception
 * handler. The security filters are switched off and the service is mocked, so nothing here touches a database or a
 * token. Creating a workspace reads the current user from the security context, so the tests put one there.
 */
@WebMvcTest(WorkspaceController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkspaceControllerTest {

    private static final UUID CURRENT_USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WorkspaceService workspaceService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CURRENT_USER_ID.toString(), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Workspace workspace(UUID id, String name, UUID ownerId) {
        User owner = Instancio.of(User.class).set(field(User::getId), ownerId).create();
        return Instancio.of(Workspace.class)
                .set(field(Workspace::getId), id)
                .set(field(Workspace::getName), name)
                .set(field(Workspace::getOwner), owner)
                .create();
    }

    @Nested
    class CreateWorkspace {

        private String body(WorkspaceCreateRequest request) {
            return objectMapper.writeValueAsString(request);
        }

        @Test
        void should_createTheWorkspaceForTheCurrentUser_whenRequestIsValid() throws Exception {
            UUID id = UUID.randomUUID();
            when(workspaceService.createWorkspace(any()))
                    .thenReturn(workspace(id, "Doe Workspace", CURRENT_USER_ID));

            mockMvc.perform(post("/workspaces").contentType(MediaType.APPLICATION_JSON)
                            .content(body(new WorkspaceCreateRequest("Doe Workspace"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.name").value("Doe Workspace"))
                    .andExpect(jsonPath("$.ownerId").value(CURRENT_USER_ID.toString()))
                    .andExpect(jsonPath("$.createdAt").exists());

            ArgumentCaptor<WorkspaceCreateCommand> commandCaptor = ArgumentCaptor.forClass(WorkspaceCreateCommand.class);
            verify(workspaceService).createWorkspace(commandCaptor.capture());
            assertEquals("Doe Workspace", commandCaptor.getValue().name());
            assertEquals(CURRENT_USER_ID, commandCaptor.getValue().ownerId());
        }

        @Test
        void should_return400WithFieldErrors_whenNameIsBlank() throws Exception {
            mockMvc.perform(post("/workspaces").contentType(MediaType.APPLICATION_JSON)
                            .content(body(new WorkspaceCreateRequest(" "))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors.name").isNotEmpty());

            verifyNoInteractions(workspaceService);
        }

        @Test
        void should_return400WithFieldErrors_whenNameIsMissing() throws Exception {
            mockMvc.perform(post("/workspaces").contentType(MediaType.APPLICATION_JSON)
                            .content(body(new WorkspaceCreateRequest(null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors.name").isNotEmpty());

            verifyNoInteractions(workspaceService);
        }

        @Test
        void should_return404_whenTheOwnerDoesNotExist() throws Exception {
            when(workspaceService.createWorkspace(any()))
                    .thenThrow(new NotFoundException("User", CURRENT_USER_ID));

            mockMvc.perform(post("/workspaces").contentType(MediaType.APPLICATION_JSON)
                            .content(body(new WorkspaceCreateRequest("Doe Workspace"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }
    }

    @Nested
    class GetWorkspace {

        @Test
        void should_returnTheWorkspace_whenWorkspaceExists() throws Exception {
            UUID id = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            when(workspaceService.getWorkspaceById(id)).thenReturn(workspace(id, "Doe Workspace", ownerId));

            mockMvc.perform(get("/workspaces/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.name").value("Doe Workspace"))
                    .andExpect(jsonPath("$.ownerId").value(ownerId.toString()));
        }

        @Test
        void should_return404_whenWorkspaceDoesNotExist() throws Exception {
            UUID id = UUID.randomUUID();
            when(workspaceService.getWorkspaceById(id)).thenThrow(new NotFoundException("Workspace", id));

            mockMvc.perform(get("/workspaces/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        void should_return400_whenWorkspaceIdIsNotAUuid() throws Exception {
            mockMvc.perform(get("/workspaces/not-a-uuid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"));

            verifyNoInteractions(workspaceService);
        }
    }

    @Nested
    class PatchWorkspace {

        private String body(WorkspacePatchRequest request) {
            return objectMapper.writeValueAsString(request);
        }

        @Test
        void should_passThePatchToTheServiceAndReturnTheWorkspace_whenRequestIsValid() throws Exception {
            UUID id = UUID.randomUUID();
            UUID newOwnerId = UUID.randomUUID();
            when(workspaceService.patchWorkspace(any(), any()))
                    .thenReturn(workspace(id, "Other Doe Workspace", newOwnerId));

            mockMvc.perform(patch("/workspaces/{id}", id).contentType(MediaType.APPLICATION_JSON)
                            .content(body(new WorkspacePatchRequest("Other Doe Workspace", newOwnerId))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Other Doe Workspace"))
                    .andExpect(jsonPath("$.ownerId").value(newOwnerId.toString()));

            ArgumentCaptor<WorkspacePatchCommand> commandCaptor = ArgumentCaptor.forClass(WorkspacePatchCommand.class);
            verify(workspaceService).patchWorkspace(any(), commandCaptor.capture());
            assertEquals("Other Doe Workspace", commandCaptor.getValue().name());
            assertEquals(newOwnerId, commandCaptor.getValue().ownerId());
        }

        @Test
        void should_leaveTheOwnerUnset_whenOnlyTheNameIsGiven() throws Exception {
            UUID id = UUID.randomUUID();
            when(workspaceService.patchWorkspace(any(), any()))
                    .thenReturn(workspace(id, "Other Doe Workspace", CURRENT_USER_ID));

            mockMvc.perform(patch("/workspaces/{id}", id).contentType(MediaType.APPLICATION_JSON)
                            .content(body(new WorkspacePatchRequest("Other Doe Workspace", null))))
                    .andExpect(status().isOk());

            ArgumentCaptor<WorkspacePatchCommand> commandCaptor = ArgumentCaptor.forClass(WorkspacePatchCommand.class);
            verify(workspaceService).patchWorkspace(any(), commandCaptor.capture());
            assertEquals("Other Doe Workspace", commandCaptor.getValue().name());
            assertNull(commandCaptor.getValue().ownerId());
        }

        @Test
        void should_return404_whenWorkspaceDoesNotExist() throws Exception {
            UUID id = UUID.randomUUID();
            when(workspaceService.patchWorkspace(any(), any())).thenThrow(new NotFoundException("Workspace", id));

            mockMvc.perform(patch("/workspaces/{id}", id).contentType(MediaType.APPLICATION_JSON)
                            .content(body(new WorkspacePatchRequest("Other Doe Workspace", null))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }
    }

    @Nested
    class DeleteWorkspace {

        @Test
        void should_return204AndDeleteTheWorkspace_whenWorkspaceExists() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/workspaces/{id}", id))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(workspaceService).deleteWorkspace(id);
        }

        @Test
        void should_return404_whenWorkspaceDoesNotExist() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new NotFoundException("Workspace", id)).when(workspaceService).deleteWorkspace(id);

            mockMvc.perform(delete("/workspaces/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }
    }
}
