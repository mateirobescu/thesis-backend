package com.mateirobescu.thesis.workspaces;

import com.mateirobescu.thesis.exception.NotFoundException;
import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.users.UserService;
import org.hibernate.jdbc.Work;
import org.instancio.Instancio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private WorkspaceService service;

    @Captor
    private ArgumentCaptor<Workspace> workspaceCaptor;

    @Nested
    class CreateWorkspace {

        @Test
        void should_returnWorkspace_whenCommandIsValid() {
            WorkspaceCreateCommand command = Instancio.create(WorkspaceCreateCommand.class);
            Workspace persisted = Instancio.create(Workspace.class);
            when(userService.getUserById(command.ownerId()))
                    .thenReturn(Instancio.of(User.class).set(field(User::getId), command.ownerId()).create());
            when(workspaceRepository.save(any(Workspace.class))).thenReturn(persisted);

            Workspace result = service.createWorkspace(command);
            assertSame(persisted, result);

            verify(workspaceRepository).save(workspaceCaptor.capture());
            Workspace saved = workspaceCaptor.getValue();

            assertEquals(command.name(), saved.getName());
            assertEquals(command.ownerId(), saved.getOwner().getId());
            assertNull(saved.getDeletedAt());
        }

        @Test
        void should_notSave_whenOwnerDoesntExist() {
            WorkspaceCreateCommand command = Instancio.create(WorkspaceCreateCommand.class);
            when(userService.getUserById(command.ownerId()))
                    .thenThrow(new NotFoundException("User", command.ownerId()));

            assertThrows(NotFoundException.class, () -> service.createWorkspace(command));

            verify(workspaceRepository, never()).save(any());
        }
    }

    @Nested
    class GetWorkspaceById {

        @Test
        void should_returnWorkspace_whenWorkspaceExists() {
            Workspace workspace = Instancio.create(Workspace.class);
            when(workspaceRepository.findByIdAndDeletedAtIsNull(workspace.getId())).thenReturn(Optional.of(workspace));

            Workspace result = service.getWorkspaceById(workspace.getId());

            assertSame(workspace, result);
        }

        @Test
        void should_throwNotFound_whenWorkspaceDoesntExist() {
            UUID workspaceId = UUID.randomUUID();
            when(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getWorkspaceById(workspaceId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(workspaceId.toString());
        }
    }

    @Nested
    class PatchWorkspace {

        @Test
        void should_returnWorkspace_WhenPatchCommandIsValid() {
            Workspace workspace = Instancio.create(Workspace.class);
            WorkspacePatchCommand command = Instancio.create(WorkspacePatchCommand.class);
            Workspace persisted = Instancio.create(Workspace.class);
            when(workspaceRepository.findByIdAndDeletedAtIsNull(workspace.getId()))
                    .thenReturn(Optional.of(workspace));
            when(userService.getUserById(command.ownerId()))
                    .thenReturn(Instancio.of(User.class).set(field(User::getId), command.ownerId()).create());
            when(workspaceRepository.save(any(Workspace.class))).thenReturn(persisted);

            Workspace result = service.patchWorkspace(workspace.getId(), command);
            assertSame(persisted, result);

            verify(workspaceRepository).save(workspaceCaptor.capture());
            Workspace saved = workspaceCaptor.getValue();

            assertEquals(command.name(), saved.getName());
            assertEquals(command.ownerId(), saved.getOwner().getId());
        }

        @Test
        void should_notLookUpOwner_whenOwnerIsNotPatched() {
            Workspace workspace = Instancio.create(Workspace.class);
            User originalOwner = workspace.getOwner();
            WorkspacePatchCommand command = Instancio.of(WorkspacePatchCommand.class)
                    .set(field(WorkspacePatchCommand::ownerId), null)
                    .create();
            Workspace persisted = Instancio.create(Workspace.class);
            when(workspaceRepository.findByIdAndDeletedAtIsNull(workspace.getId()))
                    .thenReturn(Optional.of(workspace));
            when(workspaceRepository.save(any(Workspace.class))).thenReturn(persisted);

            Workspace result = service.patchWorkspace(workspace.getId(), command);
            assertSame(persisted, result);

            verify(userService, never()).getUserById(any());
            verify(workspaceRepository).save(workspaceCaptor.capture());
            Workspace saved = workspaceCaptor.getValue();
            assertEquals(command.name(), saved.getName());
            assertSame(originalOwner, saved.getOwner());
        }

        @Test
        void should_keepName_whenNameIsNotPatched() {
            Workspace workspace = Instancio.create(Workspace.class);
            String originalName = workspace.getName();
            WorkspacePatchCommand command = Instancio.of(WorkspacePatchCommand.class)
                    .set(field(WorkspacePatchCommand::name), null)
                    .create();
            User newOwner = Instancio.of(User.class).set(field(User::getId), command.ownerId()).create();
            Workspace persisted = Instancio.create(Workspace.class);
            when(workspaceRepository.findByIdAndDeletedAtIsNull(workspace.getId()))
                    .thenReturn(Optional.of(workspace));
            when(userService.getUserById(command.ownerId())).thenReturn(newOwner);
            when(workspaceRepository.save(any(Workspace.class))).thenReturn(persisted);

            Workspace result = service.patchWorkspace(workspace.getId(), command);
            assertSame(persisted, result);

            verify(workspaceRepository).save(workspaceCaptor.capture());
            Workspace saved = workspaceCaptor.getValue();
            assertEquals(originalName, saved.getName());
            assertSame(newOwner, saved.getOwner());
        }

        @Test
        void should_notSave_whenWorkspaceDoesntExist() {
            UUID workspaceId = UUID.randomUUID();
            WorkspacePatchCommand command = Instancio.create(WorkspacePatchCommand.class);
            when(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.patchWorkspace(workspaceId, command));

            verify(userService, never()).getUserById(any());
            verify(workspaceRepository, never()).save(any());
        }

        @Test
        void should_notSave_whenNewOwnerDoesntExist() {
            Workspace workspace = Instancio.create(Workspace.class);
            WorkspacePatchCommand command = Instancio.create(WorkspacePatchCommand.class);
            when(workspaceRepository.findByIdAndDeletedAtIsNull(workspace.getId()))
                    .thenReturn(Optional.of(workspace));
            when(userService.getUserById(command.ownerId()))
                    .thenThrow(new NotFoundException("User", command.ownerId()));

            assertThrows(NotFoundException.class, () -> service.patchWorkspace(workspace.getId(), command));

            verify(workspaceRepository, never()).save(any());
        }

    }

    @Nested
    class DeleteWorkspace {

        @Test
        void should_markDeletedAndSave_whenWorkspaceExists() {
            Workspace workspace = Instancio.of(Workspace.class)
                    .set(field(Workspace::getDeletedAt), null)
                    .create();
            when(workspaceRepository.findByIdAndDeletedAtIsNull(workspace.getId()))
                    .thenReturn(Optional.of(workspace));

            service.deleteWorkspace(workspace.getId());

            verify(workspaceRepository).save(workspaceCaptor.capture());
            Workspace saved = workspaceCaptor.getValue();
            assertSame(workspace, saved);
            assertNotNull(saved.getDeletedAt());
        }

        @Test
        void should_notSave_whenWorkspaceDoesntExist() {
            UUID workspaceId = UUID.randomUUID();
            when(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.deleteWorkspace(workspaceId));

            verify(workspaceRepository, never()).save(any());
        }
    }

}
