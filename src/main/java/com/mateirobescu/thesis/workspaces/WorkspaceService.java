package com.mateirobescu.thesis.workspaces;

import com.mateirobescu.thesis.exception.NotFoundException;
import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.users.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Workspaces: creation, lookup, patching and soft deletion. A workspace belongs to one owner.
 */
@Service
@Slf4j
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UserService userService;

    public WorkspaceService(WorkspaceRepository workspaceRepository, UserService userService) {
        this.workspaceRepository = workspaceRepository;
        this.userService = userService;
    }

    /**
     * Creates a workspace owned by the command's owner.
     *
     * @return the saved workspace
     * @throws NotFoundException if the owner is not an active, verified user
     */
    public Workspace createWorkspace(WorkspaceCreateCommand command) {
        User user = userService.getUserById(command.ownerId());
        Workspace workspace = Workspace.builder()
                .name(command.name())
                .owner(user)
                .build();

        Workspace savedWorkspace = workspaceRepository.save(workspace);
        log.info("Workspace created id={}", savedWorkspace.getId());
        return savedWorkspace;
    }

    /**
     * Returns a workspace that has not been soft-deleted.
     *
     * @throws NotFoundException if there is no such workspace, including when it is soft-deleted
     */
    public Workspace getWorkspaceById(UUID id) {
        return workspaceRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(() -> new NotFoundException("Workspace", id));
    }

    /**
     * Returns the workspaces owned by a user that are not soft-deleted, looked up by id.
     *
     * @throws NotFoundException if the user is not an active, verified user
     */
    public List<Workspace> getWorkspacesByUser(UUID userId) {
        User owner = userService.getUserById(userId);
        return this.getWorkspacesByUser(owner);
    }

    /**
     * Returns the workspaces owned by an already loaded user that are not soft-deleted.
     */
    public List<Workspace> getWorkspacesByUser(User owner) {
        return workspaceRepository.findByOwnerAndDeletedAtIsNull(owner);
    }

    /**
     * Changes the name and/or the owner of a workspace; {@code null} fields in the command are left as they are.
     * The new owner is only looked up when the command has an owner id.
     *
     * @return the saved workspace
     * @throws NotFoundException if the workspace does not exist, or the new owner is not an active, verified user
     */
    public Workspace patchWorkspace(UUID id, WorkspacePatchCommand command) {
        Workspace currentWorkspace = this.getWorkspaceById(id);
        User newOwner = command.ownerId() == null ? null : userService.getUserById(command.ownerId());

        currentWorkspace.applyPatch(new WorkspacePatch(command.name(), newOwner));

        Workspace savedWorkspace = workspaceRepository.save(currentWorkspace);
        log.info("Workspace patched id={}", currentWorkspace.getId());
        return savedWorkspace;
    }

    /**
     * Soft-deletes a workspace by setting {@code deletedAt}; the row is kept and the projects inside it are not
     * changed.
     *
     * @throws NotFoundException if the workspace does not exist
     */
    public void deleteWorkspace(UUID id) {
        //TODO soft-delete the workspace's projects (and their files) as well, or decide that they are kept
        Workspace workspaceToDelete = this.getWorkspaceById(id);
        workspaceToDelete.markDeleted();
        workspaceRepository.save(workspaceToDelete);
        log.info("Workspace marked deleted id={}", id);
    }
}
