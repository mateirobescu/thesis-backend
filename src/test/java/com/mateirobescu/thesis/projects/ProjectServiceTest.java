package com.mateirobescu.thesis.projects;

import com.mateirobescu.thesis.exception.ConflictException;
import com.mateirobescu.thesis.exception.NotFoundException;
import com.mateirobescu.thesis.support.MockDatabaseException;
import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.users.UserService;
import com.mateirobescu.thesis.workspaces.Workspace;
import com.mateirobescu.thesis.workspaces.WorkspaceService;
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
public class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ProjectService service;

    @Captor
    ArgumentCaptor<Project> projectCaptor;

    @Nested
    class CreateProject {

        @Test
        void should_returnProject_whenCommandIsValid() {
            ProjectCreateCommand command = Instancio.create(ProjectCreateCommand.class);
            User owner = Instancio.of(User.class).set(field(User::getId), command.ownerId()).create();
            Workspace workspace = Instancio.of(Workspace.class).set(field(Workspace::getId), command.workspaceId()).create();
            Project persisted = Instancio.create(Project.class);
            when(userService.getUserById(command.ownerId())).thenReturn(owner);
            when(workspaceService.getWorkspaceById(command.workspaceId())).thenReturn(workspace);
            when(projectRepository.save(any(Project.class))).thenReturn(persisted);

            Project result = service.createProject(command);
            assertSame(persisted, result);

            verify(projectRepository).save(projectCaptor.capture());
            Project saved = projectCaptor.getValue();
            assertEquals(command.name(), saved.getName());
            assertEquals(command.path(), saved.getPath());
            assertSame(owner, saved.getOwner());
            assertSame(workspace, saved.getWorkspace());
        }

        @Test
        void should_useDefaultPath_whenPathIsNotProvided() {
            ProjectCreateCommand command = Instancio.of(ProjectCreateCommand.class)
                    .set(field(ProjectCreateCommand::path), null)
                    .create();
            User owner = Instancio.of(User.class).set(field(User::getId), command.ownerId()).create();
            Workspace workspace = Instancio.of(Workspace.class).set(field(Workspace::getId), command.workspaceId()).create();
            when(userService.getUserById(command.ownerId())).thenReturn(owner);
            when(workspaceService.getWorkspaceById(command.workspaceId())).thenReturn(workspace);

            Project persisted = Instancio.create(Project.class);
            when(projectRepository.save(any(Project.class))).thenReturn(persisted);

            service.createProject(command);

            verify(projectRepository)
                    .existsByWorkspace_IdAndPathAndNameAndDeletedAtIsNull(command.workspaceId(), "/", command.name());
            verify(projectRepository).save(projectCaptor.capture());
            assertEquals("/", projectCaptor.getValue().getPath());
        }

        @Test
        void should_throwConflict_whenProjectAlreadyExistsInWorkspace() {
            ProjectCreateCommand command = Instancio.create(ProjectCreateCommand.class);
            when(projectRepository.existsByWorkspace_IdAndPathAndNameAndDeletedAtIsNull(
                    command.workspaceId(), command.path(), command.name())).thenReturn(true);

            assertThatThrownBy(() -> service.createProject(command))
                    .isExactlyInstanceOf(ConflictException.class)
                    .hasMessageContaining("Project already exists in this workspace");

            verify(projectRepository, never()).save(any());
        }

        @Test
        void should_notSave_whenOwnerDoesntExist() {
            ProjectCreateCommand command = Instancio.create(ProjectCreateCommand.class);
            when(userService.getUserById(command.ownerId()))
                    .thenThrow(new NotFoundException("User", command.ownerId()));

            assertThrows(NotFoundException.class, () -> service.createProject(command));

            verify(projectRepository, never()).save(any());
        }

        @Test
        void should_notSave_whenWorkspaceDoesntExist() {
            ProjectCreateCommand command = Instancio.create(ProjectCreateCommand.class);
            User owner = Instancio.of(User.class).set(field(User::getId), command.ownerId()).create();
            when(userService.getUserById(command.ownerId())).thenReturn(owner);
            when(workspaceService.getWorkspaceById(command.workspaceId()))
                    .thenThrow(new NotFoundException("Workspace", command.workspaceId()));

            assertThrows(NotFoundException.class, () -> service.createProject(command));

            verify(projectRepository, never()).save(any());
        }

        @Test
        void should_throwConflict_whenProjectWasCreatedDuringThisCreation() {
            ProjectCreateCommand command = Instancio.create(ProjectCreateCommand.class);
            User owner = Instancio.of(User.class).set(field(User::getId), command.ownerId()).create();
            Workspace workspace = Instancio.of(Workspace.class).set(field(Workspace::getId), command.workspaceId()).create();
            when(userService.getUserById(command.ownerId())).thenReturn(owner);
            when(workspaceService.getWorkspaceById(command.workspaceId())).thenReturn(workspace);
            when(projectRepository.save(any(Project.class)))
                    .thenThrow(MockDatabaseException.constraintViolation(ProjectConstraints.WORKSPACE_PATH_NAME_UNIQUE_CONSTRAINT));

            assertThatThrownBy(() -> service.createProject(command))
                    .isExactlyInstanceOf(ConflictException.class)
                    .hasMessageContaining("A project with this path already exists");
        }

        @Test
        void should_rethrowError_whenSaveFailsForAnotherReason() {
            ProjectCreateCommand command = Instancio.create(ProjectCreateCommand.class);
            User owner = Instancio.of(User.class).set(field(User::getId), command.ownerId()).create();
            Workspace workspace = Instancio.of(Workspace.class).set(field(Workspace::getId), command.workspaceId()).create();
            DataIntegrityViolationException otherViolation = MockDatabaseException.otherViolation();
            when(userService.getUserById(command.ownerId())).thenReturn(owner);
            when(workspaceService.getWorkspaceById(command.workspaceId())).thenReturn(workspace);
            when(projectRepository.save(any(Project.class))).thenThrow(otherViolation);

            assertThatThrownBy(() -> service.createProject(command))
                    .isSameAs(otherViolation);
        }
    }

    @Nested
    class GetProjectById {

        @Test
        void should_returnProject_whenProjectExists() {
            //TODO should watch with instancio for nested objects creation
            Project project = Instancio.create(Project.class);
            when(projectRepository.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));

            Project fetched = service.getProjectById(project.getId());
            assertSame(project, fetched);
        }

        @Test
        void should_throwNotFound_whenProjectDoesntExist() {
            UUID id = UUID.randomUUID();
            when(projectRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getProjectById(id))
                    .isExactlyInstanceOf(NotFoundException.class)
                    .hasMessageContaining(id.toString());
        }
    }

    @Nested
    class GetProjectsByWorkspace {

        @Test
        void should_returnProjectsOfWorkspace_whenWorkspaceExists() {
            Workspace workspace = Instancio.create(Workspace.class);
            List<Project> projects = Instancio.createList(Project.class);
            when(workspaceService.getWorkspaceById(workspace.getId())).thenReturn(workspace);
            when(projectRepository.findByWorkspaceAndDeletedAtIsNull(workspace)).thenReturn(projects);

            List<Project> result = service.getProjectsByWorkspace(workspace.getId());

            assertSame(projects, result);
        }

        @Test
        void should_notQueryProjects_whenWorkspaceDoesntExist() {
            UUID workspaceId = UUID.randomUUID();
            when(workspaceService.getWorkspaceById(workspaceId))
                    .thenThrow(new NotFoundException("Workspace", workspaceId));

            assertThrows(NotFoundException.class, () -> service.getProjectsByWorkspace(workspaceId));

            verify(projectRepository, never()).findByWorkspaceAndDeletedAtIsNull(any());
        }
    }

    @Nested
    class PatchProject {

        @Test
        void should_returnPatchedProject_whenCommandIsValid() {
            Project project = Instancio.create(Project.class);
            ProjectPatchCommand command = Instancio.create(ProjectPatchCommand.class);
            User newOwner = Instancio.of(User.class).set(field(User::getId), command.ownerId()).create();
            Workspace newWorkspace = Instancio.of(Workspace.class).set(field(Workspace::getId), command.workspaceId()).create();
            Project persisted = Instancio.create(Project.class);
            when(projectRepository.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));
            when(userService.getUserById(command.ownerId())).thenReturn(newOwner);
            when(workspaceService.getWorkspaceById(command.workspaceId())).thenReturn(newWorkspace);
            when(projectRepository.save(any(Project.class))).thenReturn(persisted);

            Project result = service.patchProject(project.getId(), command);
            assertSame(persisted, result);

            verify(projectRepository).save(projectCaptor.capture());
            Project saved = projectCaptor.getValue();
            assertEquals(command.name(), saved.getName());
            assertEquals(command.path(), saved.getPath());
            assertSame(newOwner, saved.getOwner());
            assertSame(newWorkspace, saved.getWorkspace());
        }

        @Test
        void should_notSave_whenProjectDoesntExist() {
            UUID projectId = UUID.randomUUID();
            ProjectPatchCommand command = Instancio.create(ProjectPatchCommand.class);
            when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.patchProject(projectId, command));

            verify(projectRepository, never())
                    .existsByWorkspace_IdAndPathAndNameAndDeletedAtIsNull(any(), any(), any());
            verify(projectRepository, never()).save(any());
        }

        @Test
        void should_notSave_whenNewOwnerDoesntExist() {
            Project project = Instancio.create(Project.class);
            ProjectPatchCommand command = Instancio.of(ProjectPatchCommand.class)
                    .set(field(ProjectPatchCommand::workspaceId), null)
                    .set(field(ProjectPatchCommand::name), null)
                    .set(field(ProjectPatchCommand::path), null)
                    .create();
            when(projectRepository.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));
            when(userService.getUserById(command.ownerId()))
                    .thenThrow(new NotFoundException("User", command.ownerId()));

            assertThrows(NotFoundException.class, () -> service.patchProject(project.getId(), command));

            verify(projectRepository, never()).save(any());
        }

        @Test
        void should_notSave_whenNewWorkspaceDoesntExist() {
            Project project = Instancio.create(Project.class);
            ProjectPatchCommand command = Instancio.of(ProjectPatchCommand.class)
                    .set(field(ProjectPatchCommand::name), null)
                    .set(field(ProjectPatchCommand::path), null)
                    .set(field(ProjectPatchCommand::ownerId), null)
                    .create();
            when(projectRepository.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));
            when(workspaceService.getWorkspaceById(command.workspaceId()))
                    .thenThrow(new NotFoundException("Workspace", command.workspaceId()));

            assertThrows(NotFoundException.class, () -> service.patchProject(project.getId(), command));

            verify(projectRepository, never()).save(any());
        }

        @Test
        void should_throwConflict_whenLocationBecameUnavailableWhilePatching() {
            Project project = Instancio.create(Project.class);
            ProjectPatchCommand command = Instancio.of(ProjectPatchCommand.class)
                    .set(field(ProjectPatchCommand::workspaceId), null)
                    .set(field(ProjectPatchCommand::path), null)
                    .set(field(ProjectPatchCommand::ownerId), null)
                    .create();
            when(projectRepository.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));
            when(projectRepository.save(any(Project.class)))
                    .thenThrow(MockDatabaseException.constraintViolation(ProjectConstraints.WORKSPACE_PATH_NAME_UNIQUE_CONSTRAINT));

            assertThatThrownBy(() -> service.patchProject(project.getId(), command))
                    .isExactlyInstanceOf(ConflictException.class)
                    .hasMessageContaining("A project with this path already exists");
        }

        @Test
        void should_rethrowError_whenSaveFailsForAnotherReason() {
            Project project = Instancio.create(Project.class);
            ProjectPatchCommand command = Instancio.of(ProjectPatchCommand.class)
                    .set(field(ProjectPatchCommand::workspaceId), null)
                    .set(field(ProjectPatchCommand::path), null)
                    .set(field(ProjectPatchCommand::ownerId), null)
                    .create();
            DataIntegrityViolationException otherViolation = MockDatabaseException.otherViolation();
            when(projectRepository.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));
            when(projectRepository.save(any(Project.class))).thenThrow(otherViolation);

            assertThatThrownBy(() -> service.patchProject(project.getId(), command))
                    .isSameAs(otherViolation);
        }

        @Test
        void should_throwConflict_whenRenamedProjectAlreadyExistsInWorkspace() {
            Project project = Instancio.create(Project.class);
            ProjectPatchCommand command = Instancio.of(ProjectPatchCommand.class)
                    .set(field(ProjectPatchCommand::workspaceId), null)
                    .set(field(ProjectPatchCommand::path), null)
                    .set(field(ProjectPatchCommand::ownerId), null)
                    .create();
            when(projectRepository.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));
            when(projectRepository.existsByWorkspace_IdAndPathAndNameAndDeletedAtIsNull(
                    project.getWorkspace().getId(), project.getPath(), command.name())).thenReturn(true);

            assertThatThrownBy(() -> service.patchProject(project.getId(), command))
                    .isExactlyInstanceOf(ConflictException.class)
                    .hasMessageContaining("Project already exists in this workspace");

            verify(projectRepository, never()).save(any());
        }

        @Test
        void should_throwConflict_whenMovedProjectAlreadyExistsInNewWorkspace() {
            Project project = Instancio.create(Project.class);
            UUID newWorkspaceId = UUID.randomUUID();
            ProjectPatchCommand command = Instancio.of(ProjectPatchCommand.class)
                    .set(field(ProjectPatchCommand::workspaceId), newWorkspaceId)
                    .set(field(ProjectPatchCommand::name), null)
                    .set(field(ProjectPatchCommand::path), null)
                    .set(field(ProjectPatchCommand::ownerId), null)
                    .create();
            when(projectRepository.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));
            when(projectRepository.existsByWorkspace_IdAndPathAndNameAndDeletedAtIsNull(
                    newWorkspaceId, project.getPath(), project.getName())).thenReturn(true);

            assertThatThrownBy(() -> service.patchProject(project.getId(), command))
                    .isExactlyInstanceOf(ConflictException.class)
                    .hasMessageContaining("Project already exists in this workspace");

            verify(projectRepository, never()).save(any());
        }

        @Test
        void should_notCheckLocation_whenLocationIsUnchanged() {
            Project project = Instancio.create(Project.class);
            ProjectPatchCommand command = Instancio.of(ProjectPatchCommand.class)
                    .set(field(ProjectPatchCommand::workspaceId), project.getWorkspace().getId())
                    .set(field(ProjectPatchCommand::name), project.getName())
                    .set(field(ProjectPatchCommand::path), project.getPath())
                    .set(field(ProjectPatchCommand::ownerId), null)
                    .create();
            when(projectRepository.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));
            when(workspaceService.getWorkspaceById(project.getWorkspace().getId())).thenReturn(project.getWorkspace());

            service.patchProject(project.getId(), command);

            verify(projectRepository, never())
                    .existsByWorkspace_IdAndPathAndNameAndDeletedAtIsNull(any(), any(), any());
        }

        @Test
        void should_notCheckLocation_whenOnlyOwnerIsPatched() {
            Project project = Instancio.create(Project.class);
            User newOwner = Instancio.create(User.class);
            ProjectPatchCommand command = Instancio.of(ProjectPatchCommand.class)
                    .set(field(ProjectPatchCommand::workspaceId), null)
                    .set(field(ProjectPatchCommand::name), null)
                    .set(field(ProjectPatchCommand::path), null)
                    .set(field(ProjectPatchCommand::ownerId), newOwner.getId())
                    .create();
            when(projectRepository.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));
            when(userService.getUserById(newOwner.getId())).thenReturn(newOwner);

            service.patchProject(project.getId(), command);

            verify(projectRepository, never())
                    .existsByWorkspace_IdAndPathAndNameAndDeletedAtIsNull(any(), any(), any());
        }
    }

    @Nested
    class DeleteProject {

        @Test
        void should_markDeletedAndSave_whenProjectExists() {
            Project project = Instancio.of(Project.class)
                    .set(field(Project::getDeletedAt), null)
                    .create();
            when(projectRepository.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));

            service.deleteProject(project.getId());

            verify(projectRepository).save(projectCaptor.capture());
            Project saved = projectCaptor.getValue();
            assertSame(project, saved);
            assertNotNull(saved.getDeletedAt());
        }

        @Test
        void should_notSave_whenProjectDoesntExist() {
            UUID projectId = UUID.randomUUID();
            when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.deleteProject(projectId));

            verify(projectRepository, never()).save(any());
        }
    }

    @Nested
    class GetProjectWithNewSeq {

        @Test
        void should_incrementSeqAndReturnSavedProject_whenProjectExists() {
            Project project = Instancio.of(Project.class)
                    .set(field(Project::getSeq), 41L)
                    .create();
            Project persisted = Instancio.create(Project.class);
            when(projectRepository.findByIdForUpdate(project.getId())).thenReturn(Optional.of(project));
            when(projectRepository.save(any(Project.class))).thenReturn(persisted);

            Project result = service.getProjectWithNewSeq(project.getId());
            assertSame(persisted, result);

            verify(projectRepository).save(projectCaptor.capture());
            Project saved = projectCaptor.getValue();
            assertSame(project, saved);
            assertEquals(42L, saved.getSeq());
        }

        @Test
        void should_notSave_whenProjectDoesntExist() {
            UUID projectId = UUID.randomUUID();
            when(projectRepository.findByIdForUpdate(projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getProjectWithNewSeq(projectId))
                    .isExactlyInstanceOf(NotFoundException.class)
                    .hasMessageContaining(projectId.toString());

            verify(projectRepository, never()).save(any());
        }
    }

}
