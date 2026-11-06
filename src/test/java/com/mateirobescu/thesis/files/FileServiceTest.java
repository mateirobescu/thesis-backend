package com.mateirobescu.thesis.files;

import com.mateirobescu.thesis.exception.ConflictException;
import com.mateirobescu.thesis.exception.NotFoundException;
import com.mateirobescu.thesis.projects.Project;
import com.mateirobescu.thesis.projects.ProjectService;
import com.mateirobescu.thesis.support.MockDatabaseException;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private UserService userService;

    @InjectMocks
    private FileService service;

    @Captor
    private ArgumentCaptor<File> fileCaptor;

    @Nested
    class CreateFile {

        @Test
        void should_returnFile_whenCommandIsValid() {
            FileCreateCommand command = Instancio.create(FileCreateCommand.class);
            User owner = Instancio.of(User.class).set(field(User::getId), command.ownerId()).create();
            Project project = Instancio.of(Project.class).set(field(Project::getId), command.projectId()).create();
            File persisted = Instancio.create(File.class);
            when(userService.getUserById(command.ownerId())).thenReturn(owner);
            when(projectService.getProjectById(command.projectId())).thenReturn(project);
            when(fileRepository.save(any(File.class))).thenReturn(persisted);

            File result = service.createFile(command);
            assertSame(persisted, result);

            verify(fileRepository).save(fileCaptor.capture());
            File saved = fileCaptor.getValue();
            assertEquals(command.path(), saved.getPath());
            assertSame(project, saved.getProject());
            assertSame(owner, saved.getOwner());
        }

        @Test
        void should_throwConflict_whenFileAlreadyExistsAtPath() {
            FileCreateCommand command = Instancio.create(FileCreateCommand.class);
            User owner = Instancio.of(User.class).set(field(User::getId), command.ownerId()).create();
            Project project = Instancio.of(Project.class).set(field(Project::getId), command.projectId()).create();
            File existing = Instancio.create(File.class);
            when(userService.getUserById(command.ownerId())).thenReturn(owner);
            when(projectService.getProjectById(command.projectId())).thenReturn(project);
            when(fileRepository.findByProject_IdAndPathAndDeletedAtIsNull(project.getId(), command.path()))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.createFile(command))
                    .isExactlyInstanceOf(ConflictException.class)
                    .hasMessageContaining("File already exists")
                    .extracting(e -> ((ConflictException) e).getField()).isEqualTo("path");

            verify(fileRepository, never()).save(any());
        }

        @Test
        void should_notSave_whenOwnerDoesntExist() {
            FileCreateCommand command = Instancio.create(FileCreateCommand.class);
            when(userService.getUserById(command.ownerId()))
                    .thenThrow(new NotFoundException("User", command.ownerId()));

            assertThrows(NotFoundException.class, () -> service.createFile(command));

            verify(fileRepository, never()).save(any());
        }

        @Test
        void should_notSave_whenProjectDoesntExist() {
            FileCreateCommand command = Instancio.create(FileCreateCommand.class);
            User owner = Instancio.of(User.class).set(field(User::getId), command.ownerId()).create();
            when(userService.getUserById(command.ownerId())).thenReturn(owner);
            when(projectService.getProjectById(command.projectId()))
                    .thenThrow(new NotFoundException("Project", command.projectId()));

            assertThrows(NotFoundException.class, () -> service.createFile(command));

            verify(fileRepository, never()).save(any());
        }

        @Test
        void should_throwConflict_whenFileWasCreatedDuringThisCreation() {
            FileCreateCommand command = Instancio.create(FileCreateCommand.class);
            User owner = Instancio.of(User.class).set(field(User::getId), command.ownerId()).create();
            Project project = Instancio.of(Project.class).set(field(Project::getId), command.projectId()).create();
            when(userService.getUserById(command.ownerId())).thenReturn(owner);
            when(projectService.getProjectById(command.projectId())).thenReturn(project);
            when(fileRepository.findByProject_IdAndPathAndDeletedAtIsNull(project.getId(), command.path()))
                    .thenReturn(Optional.empty());
            when(fileRepository.save(any(File.class)))
                    .thenThrow(MockDatabaseException.constraintViolation(FileConstraints.PROJECT_PATH_UNIQUE_CONSTRAINT));

            assertThatThrownBy(() -> service.createFile(command))
                    .isExactlyInstanceOf(ConflictException.class)
                    .hasMessageContaining("File already exists")
                    .extracting(e -> ((ConflictException) e).getField()).isEqualTo("path");
        }

        @Test
        void should_rethrowError_whenSaveFailsForAnotherReason() {
            FileCreateCommand command = Instancio.create(FileCreateCommand.class);
            User owner = Instancio.of(User.class).set(field(User::getId), command.ownerId()).create();
            Project project = Instancio.of(Project.class).set(field(Project::getId), command.projectId()).create();
            DataIntegrityViolationException otherViolation = MockDatabaseException.otherViolation();
            when(userService.getUserById(command.ownerId())).thenReturn(owner);
            when(projectService.getProjectById(command.projectId())).thenReturn(project);
            when(fileRepository.save(any(File.class))).thenThrow(otherViolation);

            assertThatThrownBy(() -> service.createFile(command))
                    .isSameAs(otherViolation);
        }
    }

    @Nested
    class GetFileById {

        @Test
        void should_returnFile_whenFileExistsInProject() {
            UUID projectId = UUID.randomUUID();
            File file = Instancio.create(File.class);
            when(fileRepository.findByProject_IdAndIdAndDeletedAtIsNull(projectId, file.getId()))
                    .thenReturn(Optional.of(file));

            File result = service.getFileById(projectId, file.getId());

            assertSame(file, result);
        }

        @Test
        void should_throwNotFound_whenFileDoesntExistInProject() {
            UUID projectId = UUID.randomUUID();
            UUID fileId = UUID.randomUUID();
            when(fileRepository.findByProject_IdAndIdAndDeletedAtIsNull(projectId, fileId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getFileById(projectId, fileId))
                    .isExactlyInstanceOf(NotFoundException.class)
                    .hasMessageContaining(fileId.toString());
        }
    }

    @Nested
    class FindFileByPath {

        @Test
        void should_returnFile_whenFileExistsAtPath() {
            UUID projectId = UUID.randomUUID();
            File file = Instancio.create(File.class);
            when(fileRepository.findByProject_IdAndPathAndDeletedAtIsNull(projectId, file.getPath()))
                    .thenReturn(Optional.of(file));

            Optional<File> result = service.findFileByPath(projectId, file.getPath());

            assertEquals(Optional.of(file), result);
        }

        @Test
        void should_returnEmpty_whenNoFileAtPath() {
            UUID projectId = UUID.randomUUID();
            String path = "src/Main.java";
            when(fileRepository.findByProject_IdAndPathAndDeletedAtIsNull(projectId, path))
                    .thenReturn(Optional.empty());

            Optional<File> result = service.findFileByPath(projectId, path);

            assertEquals(Optional.empty(), result);
        }
    }

    @Nested
    class GetFileByPath {

        @Test
        void should_returnFile_whenFileExistsAtPath() {
            UUID projectId = UUID.randomUUID();
            File file = Instancio.create(File.class);
            when(fileRepository.findByProject_IdAndPathAndDeletedAtIsNull(projectId, file.getPath()))
                    .thenReturn(Optional.of(file));

            File result = service.getFileByPath(projectId, file.getPath());

            assertSame(file, result);
        }

        @Test
        void should_throwNotFound_whenNoFileAtPath() {
            UUID projectId = UUID.randomUUID();
            String path = "src/Main.java";
            when(fileRepository.findByProject_IdAndPathAndDeletedAtIsNull(projectId, path))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getFileByPath(projectId, path))
                    .isExactlyInstanceOf(NotFoundException.class)
                    .hasMessageContaining(path);
        }
    }

    @Nested
    class GetFilesByProject {

        @Test
        void should_returnFilesOfProject_whenProjectExists() {
            Project project = Instancio.create(Project.class);
            List<File> files = Instancio.createList(File.class);
            when(projectService.getProjectById(project.getId())).thenReturn(project);
            when(fileRepository.findByProjectAndDeletedAtIsNull(project)).thenReturn(files);

            List<File> result = service.getFilesByProject(project.getId());

            assertSame(files, result);
        }

        @Test
        void should_notQueryFiles_whenProjectDoesntExist() {
            UUID projectId = UUID.randomUUID();
            when(projectService.getProjectById(projectId))
                    .thenThrow(new NotFoundException("Project", projectId));

            assertThrows(NotFoundException.class, () -> service.getFilesByProject(projectId));

            verify(fileRepository, never()).findByProjectAndDeletedAtIsNull(any());
        }
    }

    @Nested
    class PatchFile {

        @Test
        void should_returnPatchedFile_whenCommandIsValid() {
            UUID projectId = UUID.randomUUID();
            File file = Instancio.create(File.class);
            FilePatchCommand command = Instancio.create(FilePatchCommand.class);
            User newOwner = Instancio.of(User.class).set(field(User::getId), command.ownerId()).create();
            File persisted = Instancio.create(File.class);
            when(fileRepository.findByProject_IdAndIdAndDeletedAtIsNull(projectId, file.getId()))
                    .thenReturn(Optional.of(file));
            when(userService.getUserById(command.ownerId())).thenReturn(newOwner);
            when(fileRepository.save(any(File.class))).thenReturn(persisted);

            File result = service.patchFile(projectId, file.getId(), command);
            assertSame(persisted, result);

            verify(fileRepository).save(fileCaptor.capture());
            File saved = fileCaptor.getValue();
            assertEquals(command.path(), saved.getPath());
            assertSame(newOwner, saved.getOwner());
        }

        @Test
        void should_throwConflict_whenNewPathAlreadyExistsInProject() {
            UUID projectId = UUID.randomUUID();
            File file = Instancio.create(File.class);
            File other = Instancio.create(File.class);
            FilePatchCommand command = Instancio.of(FilePatchCommand.class)
                    .set(field(FilePatchCommand::ownerId), null)
                    .create();
            when(fileRepository.findByProject_IdAndIdAndDeletedAtIsNull(projectId, file.getId()))
                    .thenReturn(Optional.of(file));
            when(fileRepository.findByProject_IdAndPathAndDeletedAtIsNull(projectId, command.path()))
                    .thenReturn(Optional.of(other));

            assertThatThrownBy(() -> service.patchFile(projectId, file.getId(), command))
                    .isExactlyInstanceOf(ConflictException.class)
                    .hasMessageContaining("File already exists")
                    .extracting(e -> ((ConflictException) e).getField()).isEqualTo("path");

            verify(fileRepository, never()).save(any());
        }

        @Test
        void should_notCheckPath_whenPathIsUnchanged() {
            UUID projectId = UUID.randomUUID();
            File file = Instancio.create(File.class);
            FilePatchCommand command = Instancio.of(FilePatchCommand.class)
                    .set(field(FilePatchCommand::path), file.getPath())
                    .set(field(FilePatchCommand::ownerId), null)
                    .create();
            when(fileRepository.findByProject_IdAndIdAndDeletedAtIsNull(projectId, file.getId()))
                    .thenReturn(Optional.of(file));

            service.patchFile(projectId, file.getId(), command);

            verify(fileRepository, never()).findByProject_IdAndPathAndDeletedAtIsNull(any(), any());
            verify(fileRepository).save(file);
        }

        @Test
        void should_notCheckPath_whenPathIsNotPatched() {
            UUID projectId = UUID.randomUUID();
            File file = Instancio.create(File.class);
            FilePatchCommand command = Instancio.of(FilePatchCommand.class)
                    .set(field(FilePatchCommand::path), null)
                    .create();
            User newOwner = Instancio.of(User.class).set(field(User::getId), command.ownerId()).create();
            when(fileRepository.findByProject_IdAndIdAndDeletedAtIsNull(projectId, file.getId()))
                    .thenReturn(Optional.of(file));
            when(userService.getUserById(command.ownerId())).thenReturn(newOwner);

            service.patchFile(projectId, file.getId(), command);

            verify(fileRepository, never()).findByProject_IdAndPathAndDeletedAtIsNull(any(), any());
            verify(fileRepository).save(file);
        }

        @Test
        void should_notSave_whenFileDoesntExist() {
            UUID projectId = UUID.randomUUID();
            UUID fileId = UUID.randomUUID();
            FilePatchCommand command = Instancio.create(FilePatchCommand.class);
            when(fileRepository.findByProject_IdAndIdAndDeletedAtIsNull(projectId, fileId))
                    .thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.patchFile(projectId, fileId, command));

            verify(fileRepository, never()).save(any());
        }

        @Test
        void should_notSave_whenNewOwnerDoesntExist() {
            UUID projectId = UUID.randomUUID();
            File file = Instancio.create(File.class);
            FilePatchCommand command = Instancio.create(FilePatchCommand.class);
            when(fileRepository.findByProject_IdAndIdAndDeletedAtIsNull(projectId, file.getId()))
                    .thenReturn(Optional.of(file));
            when(userService.getUserById(command.ownerId()))
                    .thenThrow(new NotFoundException("User", command.ownerId()));

            assertThrows(NotFoundException.class, () -> service.patchFile(projectId, file.getId(), command));

            verify(fileRepository, never()).save(any());
        }

        @Test
        void should_throwConflict_whenPathBecameUnavailableWhilePatching() {
            UUID projectId = UUID.randomUUID();
            File file = Instancio.create(File.class);
            FilePatchCommand command = Instancio.of(FilePatchCommand.class)
                    .set(field(FilePatchCommand::ownerId), null)
                    .create();
            when(fileRepository.findByProject_IdAndIdAndDeletedAtIsNull(projectId, file.getId()))
                    .thenReturn(Optional.of(file));
            when(fileRepository.findByProject_IdAndPathAndDeletedAtIsNull(projectId, command.path()))
                    .thenReturn(Optional.empty());
            when(fileRepository.save(any(File.class)))
                    .thenThrow(MockDatabaseException.constraintViolation(FileConstraints.PROJECT_PATH_UNIQUE_CONSTRAINT));

            assertThatThrownBy(() -> service.patchFile(projectId, file.getId(), command))
                    .isExactlyInstanceOf(ConflictException.class)
                    .hasMessageContaining("File already exists")
                    .extracting(e -> ((ConflictException) e).getField()).isEqualTo("path");
        }

        @Test
        void should_rethrowError_whenSaveFailsForAnotherReason() {
            UUID projectId = UUID.randomUUID();
            File file = Instancio.create(File.class);
            FilePatchCommand command = Instancio.of(FilePatchCommand.class)
                    .set(field(FilePatchCommand::ownerId), null)
                    .create();
            DataIntegrityViolationException otherViolation = MockDatabaseException.otherViolation();
            when(fileRepository.findByProject_IdAndIdAndDeletedAtIsNull(projectId, file.getId()))
                    .thenReturn(Optional.of(file));
            when(fileRepository.save(any(File.class))).thenThrow(otherViolation);

            assertThatThrownBy(() -> service.patchFile(projectId, file.getId(), command))
                    .isSameAs(otherViolation);
        }
    }

    @Nested
    class DeleteFile {

        @Test
        void should_markDeletedAndSave_whenFileExists() {
            UUID projectId = UUID.randomUUID();
            File file = Instancio.of(File.class)
                    .set(field(File::getDeletedAt), null)
                    .create();
            when(fileRepository.findByProject_IdAndIdAndDeletedAtIsNull(projectId, file.getId()))
                    .thenReturn(Optional.of(file));

            service.deleteFile(projectId, file.getId());

            verify(fileRepository).save(fileCaptor.capture());
            File saved = fileCaptor.getValue();
            assertSame(file, saved);
            assertNotNull(saved.getDeletedAt());
        }

        @Test
        void should_notSave_whenFileDoesntExist() {
            UUID projectId = UUID.randomUUID();
            UUID fileId = UUID.randomUUID();
            when(fileRepository.findByProject_IdAndIdAndDeletedAtIsNull(projectId, fileId))
                    .thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.deleteFile(projectId, fileId));

            verify(fileRepository, never()).save(any());
        }
    }

}
