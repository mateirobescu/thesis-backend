package com.mateirobescu.thesis.projects;

import com.mateirobescu.thesis.exception.ConflictException;
import com.mateirobescu.thesis.exception.NotFoundException;
import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.workspaces.Workspace;
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
 * Checks the HTTP layer of {@code ProjectController}: routes, status codes and JSON, with the real exception
 * handler. The security filters are switched off and the service is mocked, so nothing here touches a database or a
 * token. Creating a project reads the current user from the security context, so the tests put one there.
 */
@WebMvcTest(ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectControllerTest {

    private static final UUID CURRENT_USER_ID = UUID.randomUUID();
    private static final String NAME = "Doe Project";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CURRENT_USER_ID.toString(), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Project project(UUID id, UUID workspaceId, String name, String path, UUID ownerId) {
        Workspace workspace = Instancio.of(Workspace.class).set(field(Workspace::getId), workspaceId).create();
        User owner = Instancio.of(User.class).set(field(User::getId), ownerId).create();
        return Instancio.of(Project.class)
                .set(field(Project::getId), id)
                .set(field(Project::getWorkspace), workspace)
                .set(field(Project::getName), name)
                .set(field(Project::getPath), path)
                .set(field(Project::getOwner), owner)
                .create();
    }

    @Nested
    class CreateProject {

        private String body(ProjectCreateRequest request) {
            return objectMapper.writeValueAsString(request);
        }

        @Test
        void should_createTheProjectForTheCurrentUser_whenRequestIsValid() throws Exception {
            UUID id = UUID.randomUUID();
            UUID workspaceId = UUID.randomUUID();
            Project created = project(id, workspaceId, NAME, "/archive", CURRENT_USER_ID);
            when(projectService.createProject(any())).thenReturn(created);

            mockMvc.perform(post("/projects").contentType(MediaType.APPLICATION_JSON)
                            .content(body(new ProjectCreateRequest(workspaceId, NAME, "/archive"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.workspaceId").value(workspaceId.toString()))
                    .andExpect(jsonPath("$.name").value(NAME))
                    .andExpect(jsonPath("$.path").value("/archive"))
                    .andExpect(jsonPath("$.seq").value(created.getSeq()))
                    .andExpect(jsonPath("$.ownerId").value(CURRENT_USER_ID.toString()))
                    .andExpect(jsonPath("$.createdAt").exists());

            ArgumentCaptor<ProjectCreateCommand> commandCaptor = ArgumentCaptor.forClass(ProjectCreateCommand.class);
            verify(projectService).createProject(commandCaptor.capture());
            assertEquals(workspaceId, commandCaptor.getValue().workspaceId());
            assertEquals(NAME, commandCaptor.getValue().name());
            assertEquals("/archive", commandCaptor.getValue().path());
            assertEquals(CURRENT_USER_ID, commandCaptor.getValue().ownerId());
        }

        @Test
        void should_passANullPath_whenNoPathIsGiven() throws Exception {
            UUID workspaceId = UUID.randomUUID();
            when(projectService.createProject(any()))
                    .thenReturn(project(UUID.randomUUID(), workspaceId, NAME, "/", CURRENT_USER_ID));

            mockMvc.perform(post("/projects").contentType(MediaType.APPLICATION_JSON)
                            .content(body(new ProjectCreateRequest(workspaceId, NAME, null))))
                    .andExpect(status().isCreated());

            ArgumentCaptor<ProjectCreateCommand> commandCaptor = ArgumentCaptor.forClass(ProjectCreateCommand.class);
            verify(projectService).createProject(commandCaptor.capture());
            assertNull(commandCaptor.getValue().path());
        }

        @Test
        void should_return400WithFieldErrors_whenNameIsBlank() throws Exception {
            mockMvc.perform(post("/projects").contentType(MediaType.APPLICATION_JSON)
                            .content(body(new ProjectCreateRequest(UUID.randomUUID(), " ", null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors.name").isNotEmpty());

            verifyNoInteractions(projectService);
        }

        @Test
        void should_return400WithFieldErrors_whenWorkspaceIdIsMissing() throws Exception {
            mockMvc.perform(post("/projects").contentType(MediaType.APPLICATION_JSON)
                            .content(body(new ProjectCreateRequest(null, NAME, null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors.workspaceId").isNotEmpty());

            verifyNoInteractions(projectService);
        }

        @Test
        void should_return409WithFieldErrors_whenProjectAlreadyExistsAtTheLocation() throws Exception {
            when(projectService.createProject(any()))
                    .thenThrow(new ConflictException("project", "Project already exists in this workspace"));

            mockMvc.perform(post("/projects").contentType(MediaType.APPLICATION_JSON)
                            .content(body(new ProjectCreateRequest(UUID.randomUUID(), NAME, null))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"))
                    .andExpect(jsonPath("$.errors.project[0]").value("Project already exists in this workspace"));
        }

        @Test
        void should_return404_whenTheWorkspaceDoesNotExist() throws Exception {
            UUID workspaceId = UUID.randomUUID();
            when(projectService.createProject(any())).thenThrow(new NotFoundException("Workspace", workspaceId));

            mockMvc.perform(post("/projects").contentType(MediaType.APPLICATION_JSON)
                            .content(body(new ProjectCreateRequest(workspaceId, NAME, null))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }
    }

    @Nested
    class GetProject {

        @Test
        void should_returnTheProject_whenProjectExists() throws Exception {
            UUID id = UUID.randomUUID();
            UUID workspaceId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            when(projectService.getProjectById(id)).thenReturn(project(id, workspaceId, NAME, "/", ownerId));

            mockMvc.perform(get("/projects/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.workspaceId").value(workspaceId.toString()))
                    .andExpect(jsonPath("$.name").value(NAME))
                    .andExpect(jsonPath("$.path").value("/"))
                    .andExpect(jsonPath("$.ownerId").value(ownerId.toString()));
        }

        @Test
        void should_return404_whenProjectDoesNotExist() throws Exception {
            UUID id = UUID.randomUUID();
            when(projectService.getProjectById(id)).thenThrow(new NotFoundException("Project", id));

            mockMvc.perform(get("/projects/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        void should_return400_whenProjectIdIsNotAUuid() throws Exception {
            mockMvc.perform(get("/projects/not-a-uuid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"));

            verifyNoInteractions(projectService);
        }
    }

    @Nested
    class PatchProject {

        private String body(ProjectPatchRequest request) {
            return objectMapper.writeValueAsString(request);
        }

        @Test
        void should_passThePatchToTheServiceAndReturnTheProject_whenRequestIsValid() throws Exception {
            UUID id = UUID.randomUUID();
            UUID newWorkspaceId = UUID.randomUUID();
            UUID newOwnerId = UUID.randomUUID();
            when(projectService.patchProject(any(), any()))
                    .thenReturn(project(id, newWorkspaceId, "Other Doe Project", "/archive", newOwnerId));

            mockMvc.perform(patch("/projects/{id}", id).contentType(MediaType.APPLICATION_JSON)
                            .content(body(new ProjectPatchRequest(newWorkspaceId, "Other Doe Project", "/archive", newOwnerId))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.workspaceId").value(newWorkspaceId.toString()))
                    .andExpect(jsonPath("$.name").value("Other Doe Project"))
                    .andExpect(jsonPath("$.path").value("/archive"))
                    .andExpect(jsonPath("$.ownerId").value(newOwnerId.toString()));

            ArgumentCaptor<ProjectPatchCommand> commandCaptor = ArgumentCaptor.forClass(ProjectPatchCommand.class);
            verify(projectService).patchProject(any(), commandCaptor.capture());
            assertEquals(newWorkspaceId, commandCaptor.getValue().workspaceId());
            assertEquals("Other Doe Project", commandCaptor.getValue().name());
            assertEquals("/archive", commandCaptor.getValue().path());
            assertEquals(newOwnerId, commandCaptor.getValue().ownerId());
        }

        @Test
        void should_leaveTheOtherFieldsUnset_whenOnlyTheNameIsGiven() throws Exception {
            UUID id = UUID.randomUUID();
            when(projectService.patchProject(any(), any()))
                    .thenReturn(project(id, UUID.randomUUID(), "Other Doe Project", "/", CURRENT_USER_ID));

            mockMvc.perform(patch("/projects/{id}", id).contentType(MediaType.APPLICATION_JSON)
                            .content(body(new ProjectPatchRequest(null, "Other Doe Project", null, null))))
                    .andExpect(status().isOk());

            ArgumentCaptor<ProjectPatchCommand> commandCaptor = ArgumentCaptor.forClass(ProjectPatchCommand.class);
            verify(projectService).patchProject(any(), commandCaptor.capture());
            assertEquals("Other Doe Project", commandCaptor.getValue().name());
            assertNull(commandCaptor.getValue().workspaceId());
            assertNull(commandCaptor.getValue().path());
            assertNull(commandCaptor.getValue().ownerId());
        }

        @Test
        void should_return409WithFieldErrors_whenTheNewLocationIsTaken() throws Exception {
            when(projectService.patchProject(any(), any()))
                    .thenThrow(new ConflictException("project", "Project already exists in this workspace"));

            mockMvc.perform(patch("/projects/{id}", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                            .content(body(new ProjectPatchRequest(null, NAME, null, null))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"))
                    .andExpect(jsonPath("$.errors.project[0]").value("Project already exists in this workspace"));
        }

        @Test
        void should_return404_whenProjectDoesNotExist() throws Exception {
            UUID id = UUID.randomUUID();
            when(projectService.patchProject(any(), any())).thenThrow(new NotFoundException("Project", id));

            mockMvc.perform(patch("/projects/{id}", id).contentType(MediaType.APPLICATION_JSON)
                            .content(body(new ProjectPatchRequest(null, NAME, null, null))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }
    }

    @Nested
    class DeleteProject {

        @Test
        void should_return204AndDeleteTheProject_whenProjectExists() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/projects/{id}", id))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(projectService).deleteProject(id);
        }

        @Test
        void should_return404_whenProjectDoesNotExist() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new NotFoundException("Project", id)).when(projectService).deleteProject(id);

            mockMvc.perform(delete("/projects/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }
    }
}
