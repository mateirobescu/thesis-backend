package com.mateirobescu.thesis.projects;

import com.mateirobescu.thesis.exception.ConflictException;
import com.mateirobescu.thesis.exception.ConstraintViolations;
import com.mateirobescu.thesis.exception.NotFoundException;
import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.users.UserService;
import com.mateirobescu.thesis.workspaces.Workspace;
import com.mateirobescu.thesis.workspaces.WorkspaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Projects inside workspaces: creation, lookup, patching, soft deletion and the per-project sequence numbers used
 * by operations. Within a workspace a project is identified by its (path, name) pair.
 */
@Service
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final WorkspaceService workspaceService;
    private final UserService userService;

    public ProjectService(ProjectRepository projectRepository, WorkspaceService workspaceService, UserService userService) {
        this.projectRepository = projectRepository;
        this.workspaceService = workspaceService;
        this.userService = userService;
    }

    /**
     * Creates a project in a workspace, at the command's path or at {@code "/"} when no path is given.
     *
     * @return the saved project
     * @throws ConflictException with field {@code project} if a project (not soft-deleted) with the same workspace, path
     *                           and name already exists, whether found by the pre-check or reported by the unique
     *                           index when two creates race
     * @throws NotFoundException if the owner is not an active, verified user or the workspace does not exist
     */
    public Project createProject(ProjectCreateCommand command) {
        String path = command.path() != null ? command.path() : "/";
        if(this.projectExistsInWorkspace(command.workspaceId(), path, command.name())) {
            //TODO maybe a better error message?
            throw new ConflictException("project", "Project already exists in this workspace");
        }

        User user = userService.getUserById(command.ownerId());
        Workspace workspace = workspaceService.getWorkspaceById(command.workspaceId());
        Project project = Project.builder()
                .name(command.name())
                .workspace(workspace)
                .owner(user)
                .path(path)
                .build();

        try {
            Project savedProject = projectRepository.save(project);
            log.info("Project created id={}", savedProject.getId());
            return savedProject;
        } catch (DataIntegrityViolationException exception) {
            if(ConstraintViolations.isViolationOf(exception, ProjectConstraints.WORKSPACE_PATH_NAME_UNIQUE_CONSTRAINT)){
                log.info("Constraint violation - project create failed, a project at this location already exists: {} - {}/{}",
                        project.getWorkspace().getId(), project.getPath(), project.getName());
                throw new ConflictException("project", "A project with this path already exists");
            }
            throw exception;
        }
    }

    /**
     * Returns a project that has not been soft-deleted.
     *
     * @throws NotFoundException if there is no such project, including when it is soft-deleted
     */
    public Project getProjectById(UUID id) {
        return projectRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(() -> new NotFoundException("Project", id));
    }

    /**
     * Tells whether a project (not soft-deleted) with this path and name exists in the workspace.
     */
    public boolean projectExistsInWorkspace(UUID workspaceId, String path, String name) {
        return this.projectRepository.existsByWorkspace_IdAndPathAndNameAndDeletedAtIsNull(workspaceId, path, name);
    }

    /**
     * Returns the projects of a workspace that are not soft-deleted.
     *
     * @throws NotFoundException if the workspace does not exist
     */
    public List<Project> getProjectsByWorkspace(UUID workspaceId) {
        Workspace workspace = workspaceService.getWorkspaceById(workspaceId);
        return projectRepository.findByWorkspaceAndDeletedAtIsNull(workspace);
    }

    /**
     * Changes the fields present in the command; {@code null} fields are left as they are. The new owner and
     * workspace are only looked up when the command has their ids. The (workspace, path, name) location is checked
     * for a clash only when it actually changes.
     *
     * @return the saved project
     * @throws NotFoundException if the project does not exist, the new owner is not an active, verified user, or
     *                           the new workspace does not exist
     * @throws ConflictException with field {@code project} if the resulting location is already used by another
     *                           project that is not soft-deleted, whether found by the pre-check or reported by the unique
     *                           index when two requests race
     */
    public Project patchProject(UUID id, ProjectPatchCommand command) {
        Project project = this.getProjectById(id);
        this.assertLocationIsFree(project, command);

        User newOwner = command.ownerId() != null ? userService.getUserById(command.ownerId()) : null;
        Workspace newWorkspace = command.workspaceId() != null ?
                workspaceService.getWorkspaceById(command.workspaceId()) : null;

        project.applyPatch(new ProjectPatch(newWorkspace, command.name(), command.path(), newOwner));

        try {
            Project savedProject = projectRepository.save(project);
            log.info("Project patched id={}", project.getId());
            return savedProject;
        } catch (DataIntegrityViolationException exception) {
            if(ConstraintViolations.isViolationOf(exception, ProjectConstraints.WORKSPACE_PATH_NAME_UNIQUE_CONSTRAINT)) {
                log.info("Constraint violation - project patch failed, a project at this location already exists: {} - {}/{}",
                        project.getWorkspace().getId(), project.getPath(), project.getName());
                throw new ConflictException("project", "A project with this path already exists");
            }
            throw exception;
        }
    }

    private void assertLocationIsFree(Project project, ProjectPatchCommand command) {
        UUID workspaceId = Objects.requireNonNullElse(command.workspaceId(), project.getWorkspace().getId());
        String path = Objects.requireNonNullElse(command.path(), project.getPath());
        String name = Objects.requireNonNullElse(command.name(), project.getName());

        boolean moved = !workspaceId.equals(project.getWorkspace().getId())
                || !path.equals(project.getPath())
                || !name.equals(project.getName());

        if (moved && this.projectExistsInWorkspace(workspaceId, path, name)) {
            throw new ConflictException("project", "Project already exists in this workspace");
        }
    }

    /**
     * Soft-deletes a project by setting {@code deletedAt}; the row is kept.
     *
     * @throws NotFoundException if the project does not exist
     */
    public void deleteProject(UUID id) {
        //TODO soft-delete the project's files as well, or decide that they are kept
        Project projectToDelete = this.getProjectById(id);
        projectToDelete.markDeleted();
        projectRepository.save(projectToDelete);
        log.info("Project marked deleted id={}", id);
    }

    //TODO maybe make this clearer ??
    /**
     * Locks the project row, increments its sequence number and saves it, so concurrent callers each get a
     * distinct number. It must run inside an existing transaction ({@link Propagation#MANDATORY}), because the
     * lock is held until that transaction ends.
     *
     * @return the saved project, whose {@code seq} is the newly assigned number
     * @throws NotFoundException if the project does not exist
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Project getProjectWithNewSeq(UUID id) {
        Project project = projectRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Project", id));
        return projectRepository.save(project.incrementSeq());
    }
}
