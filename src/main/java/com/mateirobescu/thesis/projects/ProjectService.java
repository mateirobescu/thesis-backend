package com.mateirobescu.thesis.projects;

import com.mateirobescu.thesis.exception.NotFoundException;
import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.users.UserService;
import com.mateirobescu.thesis.workspaces.Workspace;
import com.mateirobescu.thesis.workspaces.WorkspacePatch;
import com.mateirobescu.thesis.workspaces.WorkspaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ProjectService {

    ProjectRepository projectRepository;
    WorkspaceService workspaceService;
    UserService userService;

    public ProjectService(ProjectRepository projectRepository, WorkspaceService workspaceService, UserService userService) {
        this.projectRepository = projectRepository;
        this.workspaceService = workspaceService;
        this.userService = userService;
    }

    public Project createProject(ProjectCreateCommand command) {
        User user = userService.getUserById(command.ownerId());
        Workspace workspace = workspaceService.getWorkspaceById(command.workspaceId());
        var builder = Project.builder()
                .name(command.name())
                .workspace(workspace)
                .owner(user);

        if(command.path() != null)
            builder.path(command.path());

        Project savedProject = projectRepository.save(builder.build());
        log.info("Project created id={}", savedProject.getId());
        return savedProject;
    }

    public Project getProjectById(UUID id) {
        return projectRepository.findById(id).orElseThrow(() -> new NotFoundException("Project", id));
    }

    public List<Project> getProjectsByWorkspace(UUID workspaceId) {
        Workspace workspace = workspaceService.getWorkspaceById(workspaceId);
        return projectRepository.findByWorkspace(workspace);
    }

    public Project patchProject(UUID id, ProjectPatchRequest request) {
        Project currentProject = this.getProjectById(id);
        User newOwner = request.ownerId() != null ? userService.getUserById(request.ownerId()) : null;
        Workspace newWorkspace = request.workspaceId() != null ? workspaceService.getWorkspaceById(request.workspaceId()) : null;

        currentProject.applyPatch(new ProjectPatch(newWorkspace, request.name(), request.path(), newOwner));

        Project savedProject = projectRepository.save(currentProject);
        log.info("Project patched id={}", currentProject.getId());
        return savedProject;
    }

    public void deleteProject(UUID id) {
        Project project = this.getProjectById(id);
        log.info("Project deleting id={}", project.getId());
        projectRepository.delete(project);
        log.info("Project deleted id={}", project.getId());
    }

    public Project getProjectWithNewSeq(UUID id) {
        Project project = projectRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Project", id));
        return projectRepository.save(project.incrementSeq());
    }
}
