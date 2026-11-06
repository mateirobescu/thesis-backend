package com.mateirobescu.thesis.files;

import com.mateirobescu.thesis.exception.ConflictException;
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

import java.util.List;
import java.util.UUID;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
 * Checks the HTTP layer of {@code FileController}: routes, status codes and JSON, with the real exception handler.
 * The security filters are switched off and the service is mocked, so nothing here touches a database or a token.
 * Creating a file reads the current user from the security context, so the tests put one there.
 */
@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false)
class FileControllerTest {

    private static final UUID CURRENT_USER_ID = UUID.randomUUID();
    private static final String PATH = "src/Main.java";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FileService fileService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CURRENT_USER_ID.toString(), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private File file(UUID id, UUID projectId, String path, UUID ownerId) {
        Project project = Instancio.of(Project.class).set(field(Project::getId), projectId).create();
        User owner = Instancio.of(User.class).set(field(User::getId), ownerId).create();
        return Instancio.of(File.class)
                .set(field(File::getId), id)
                .set(field(File::getProject), project)
                .set(field(File::getPath), path)
                .set(field(File::getOwner), owner)
                .create();
    }

    @Nested
    class CreateFile {

        private String body(FileCreateRequest request) {
            return objectMapper.writeValueAsString(request);
        }

        @Test
        void should_createTheFileForTheCurrentUser_whenRequestIsValid() throws Exception {
            UUID id = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();
            when(fileService.createFile(any())).thenReturn(file(id, projectId, PATH, CURRENT_USER_ID));

            mockMvc.perform(post("/projects/{projectId}/files", projectId).contentType(MediaType.APPLICATION_JSON)
                            .content(body(new FileCreateRequest(PATH))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                    .andExpect(jsonPath("$.path").value(PATH))
                    .andExpect(jsonPath("$.ownerId").value(CURRENT_USER_ID.toString()))
                    .andExpect(jsonPath("$.createdAt").exists());

            ArgumentCaptor<FileCreateCommand> commandCaptor = ArgumentCaptor.forClass(FileCreateCommand.class);
            verify(fileService).createFile(commandCaptor.capture());
            assertEquals(projectId, commandCaptor.getValue().projectId());
            assertEquals(PATH, commandCaptor.getValue().path());
            assertEquals(CURRENT_USER_ID, commandCaptor.getValue().ownerId());
        }

        @Test
        void should_return400WithFieldErrors_whenPathIsBlank() throws Exception {
            mockMvc.perform(post("/projects/{projectId}/files", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                            .content(body(new FileCreateRequest(" "))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors.path").isNotEmpty());

            verifyNoInteractions(fileService);
        }

        @Test
        void should_return400WithFieldErrors_whenPathIsMissing() throws Exception {
            mockMvc.perform(post("/projects/{projectId}/files", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                            .content(body(new FileCreateRequest(null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors.path").isNotEmpty());

            verifyNoInteractions(fileService);
        }

        @Test
        void should_return400WithFieldErrors_whenPathIsNotAValidPath() throws Exception {
            mockMvc.perform(post("/projects/{projectId}/files", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                            .content(body(new FileCreateRequest("../Main.java"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors.path").isNotEmpty());

            verifyNoInteractions(fileService);
        }

        @Test
        void should_return409WithFieldErrors_whenFileAlreadyExistsAtThePath() throws Exception {
            when(fileService.createFile(any())).thenThrow(new ConflictException("path", "File already exists"));

            mockMvc.perform(post("/projects/{projectId}/files", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                            .content(body(new FileCreateRequest(PATH))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"))
                    .andExpect(jsonPath("$.errors.path[0]").value("File already exists"));
        }

        @Test
        void should_return404_whenTheProjectDoesNotExist() throws Exception {
            UUID projectId = UUID.randomUUID();
            when(fileService.createFile(any())).thenThrow(new NotFoundException("Project", projectId));

            mockMvc.perform(post("/projects/{projectId}/files", projectId).contentType(MediaType.APPLICATION_JSON)
                            .content(body(new FileCreateRequest(PATH))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        void should_return400_whenProjectIdIsNotAUuid() throws Exception {
            mockMvc.perform(post("/projects/not-a-uuid/files").contentType(MediaType.APPLICATION_JSON)
                            .content(body(new FileCreateRequest(PATH))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"));

            verifyNoInteractions(fileService);
        }
    }

    @Nested
    class GetFile {

        @Test
        void should_returnTheFile_whenFileExistsInTheProject() throws Exception {
            UUID id = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            when(fileService.getFileById(projectId, id)).thenReturn(file(id, projectId, PATH, ownerId));

            mockMvc.perform(get("/projects/{projectId}/files/{id}", projectId, id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                    .andExpect(jsonPath("$.path").value(PATH))
                    .andExpect(jsonPath("$.ownerId").value(ownerId.toString()));
        }

        @Test
        void should_return404_whenFileDoesNotExistInTheProject() throws Exception {
            UUID id = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();
            when(fileService.getFileById(projectId, id)).thenThrow(new NotFoundException("File", id));

            mockMvc.perform(get("/projects/{projectId}/files/{id}", projectId, id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        void should_return400_whenFileIdIsNotAUuid() throws Exception {
            mockMvc.perform(get("/projects/{projectId}/files/not-a-uuid", UUID.randomUUID()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"));

            verifyNoInteractions(fileService);
        }
    }

    @Nested
    class GetFileByPath {

        @Test
        void should_returnTheFileAtThePath_whenFileExists() throws Exception {
            UUID projectId = UUID.randomUUID();
            File found = file(UUID.randomUUID(), projectId, PATH, UUID.randomUUID());
            when(fileService.getFileByPath(projectId, PATH)).thenReturn(found);

            mockMvc.perform(get("/projects/{projectId}/files/by-path", projectId).param("path", PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(found.getId().toString()))
                    .andExpect(jsonPath("$.path").value(PATH));

            verify(fileService, never()).getFileById(any(), any());
        }

        @Test
        void should_return404_whenNoFileIsAtThePath() throws Exception {
            UUID projectId = UUID.randomUUID();
            when(fileService.getFileByPath(projectId, PATH)).thenThrow(new NotFoundException("File", PATH));

            mockMvc.perform(get("/projects/{projectId}/files/by-path", projectId).param("path", PATH))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        void should_return400_whenThePathParameterIsMissing() throws Exception {
            mockMvc.perform(get("/projects/{projectId}/files/by-path", UUID.randomUUID()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

            verifyNoInteractions(fileService);
        }
    }

    @Nested
    class GetFiles {

        @Test
        void should_returnTheFilesOfTheProject_whenListing() throws Exception {
            UUID projectId = UUID.randomUUID();
            when(fileService.getFilesByProject(projectId)).thenReturn(List.of(
                    file(UUID.randomUUID(), projectId, PATH, UUID.randomUUID()),
                    file(UUID.randomUUID(), projectId, "src/Other.java", UUID.randomUUID())));

            mockMvc.perform(get("/projects/{projectId}/files", projectId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.files.length()").value(2))
                    .andExpect(jsonPath("$.files[0].path").value(PATH))
                    .andExpect(jsonPath("$.files[1].path").value("src/Other.java"));
        }

        @Test
        void should_returnAnEmptyList_whenTheProjectHasNoFiles() throws Exception {
            UUID projectId = UUID.randomUUID();
            when(fileService.getFilesByProject(projectId)).thenReturn(List.of());

            mockMvc.perform(get("/projects/{projectId}/files", projectId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.files.length()").value(0));
        }

        @Test
        void should_return404_whenTheProjectDoesNotExist() throws Exception {
            UUID projectId = UUID.randomUUID();
            when(fileService.getFilesByProject(projectId)).thenThrow(new NotFoundException("Project", projectId));

            mockMvc.perform(get("/projects/{projectId}/files", projectId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }
    }

    @Nested
    class PatchFile {

        private String body(FilePatchRequest request) {
            return objectMapper.writeValueAsString(request);
        }

        @Test
        void should_passThePatchToTheServiceAndReturnTheFile_whenRequestIsValid() throws Exception {
            UUID id = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();
            UUID newOwnerId = UUID.randomUUID();
            when(fileService.patchFile(any(), any(), any()))
                    .thenReturn(file(id, projectId, "src/Other.java", newOwnerId));

            mockMvc.perform(patch("/projects/{projectId}/files/{id}", projectId, id).contentType(MediaType.APPLICATION_JSON)
                            .content(body(new FilePatchRequest("src/Other.java", newOwnerId))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.path").value("src/Other.java"))
                    .andExpect(jsonPath("$.ownerId").value(newOwnerId.toString()));

            ArgumentCaptor<FilePatchCommand> commandCaptor = ArgumentCaptor.forClass(FilePatchCommand.class);
            verify(fileService).patchFile(any(), any(), commandCaptor.capture());
            assertEquals("src/Other.java", commandCaptor.getValue().path());
            assertEquals(newOwnerId, commandCaptor.getValue().ownerId());
        }

        @Test
        void should_useTheIdsFromThePath_whenPatching() throws Exception {
            UUID id = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();
            when(fileService.patchFile(any(), any(), any()))
                    .thenReturn(file(id, projectId, "src/Other.java", CURRENT_USER_ID));

            mockMvc.perform(patch("/projects/{projectId}/files/{id}", projectId, id).contentType(MediaType.APPLICATION_JSON)
                            .content(body(new FilePatchRequest("src/Other.java", null))))
                    .andExpect(status().isOk());

            ArgumentCaptor<UUID> projectIdCaptor = ArgumentCaptor.forClass(UUID.class);
            ArgumentCaptor<UUID> fileIdCaptor = ArgumentCaptor.forClass(UUID.class);
            verify(fileService).patchFile(projectIdCaptor.capture(), fileIdCaptor.capture(), any());
            assertEquals(projectId, projectIdCaptor.getValue());
            assertEquals(id, fileIdCaptor.getValue());
        }

        @Test
        void should_leaveTheOwnerUnset_whenOnlyThePathIsGiven() throws Exception {
            UUID id = UUID.randomUUID();
            when(fileService.patchFile(any(), any(), any()))
                    .thenReturn(file(id, UUID.randomUUID(), "src/Other.java", CURRENT_USER_ID));

            mockMvc.perform(patch("/projects/{projectId}/files/{id}", UUID.randomUUID(), id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(new FilePatchRequest("src/Other.java", null))))
                    .andExpect(status().isOk());

            ArgumentCaptor<FilePatchCommand> commandCaptor = ArgumentCaptor.forClass(FilePatchCommand.class);
            verify(fileService).patchFile(any(), any(), commandCaptor.capture());
            assertEquals("src/Other.java", commandCaptor.getValue().path());
            assertNull(commandCaptor.getValue().ownerId());
        }

        @Test
        void should_return400WithFieldErrors_whenPatchedPathIsNotAValidPath() throws Exception {
            mockMvc.perform(patch("/projects/{projectId}/files/{id}", UUID.randomUUID(), UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(new FilePatchRequest("../Main.java", null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors.path").isNotEmpty());

            verifyNoInteractions(fileService);
        }

        @Test
        void should_return409WithFieldErrors_whenTheNewPathIsTaken() throws Exception {
            when(fileService.patchFile(any(), any(), any())).thenThrow(new ConflictException("path", "File already exists"));

            mockMvc.perform(patch("/projects/{projectId}/files/{id}", UUID.randomUUID(), UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(new FilePatchRequest(PATH, null))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"))
                    .andExpect(jsonPath("$.errors.path[0]").value("File already exists"));
        }

        @Test
        void should_return404_whenFileDoesNotExist() throws Exception {
            UUID id = UUID.randomUUID();
            when(fileService.patchFile(any(), any(), any())).thenThrow(new NotFoundException("File", id));

            mockMvc.perform(patch("/projects/{projectId}/files/{id}", UUID.randomUUID(), id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(new FilePatchRequest(PATH, null))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }
    }

    @Nested
    class DeleteFile {

        @Test
        void should_return204AndDeleteTheFile_whenFileExists() throws Exception {
            UUID id = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();

            mockMvc.perform(delete("/projects/{projectId}/files/{id}", projectId, id))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(fileService).deleteFile(projectId, id);
        }

        @Test
        void should_return404_whenFileDoesNotExist() throws Exception {
            UUID id = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();
            doThrow(new NotFoundException("File", id)).when(fileService).deleteFile(projectId, id);

            mockMvc.perform(delete("/projects/{projectId}/files/{id}", projectId, id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }
    }
}
