package com.mateirobescu.thesis.projects;

import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.users.UserService;
import com.mateirobescu.thesis.workspaces.Workspace;
import com.mateirobescu.thesis.workspaces.WorkspacePatch;
import com.mateirobescu.thesis.workspaces.WorkspaceService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    ProjectRepository projectRepository;
    WorkspaceService workspaceService;
    UserService userService;

    public ProjectService(ProjectRepository projectRepository, WorkspaceService workspaceService, UserService userService) {
        this.projectRepository = projectRepository;
        this.workspaceService = workspaceService;
        this.userService = userService;
    }

    public Project createProject(String name, String path, UUID workspaceId, UUID userId) {
        User user = userService.getUserById(userId);
        Workspace workspace = workspaceService.getWorkspaceById(workspaceId);
        var builder = Project.builder()
                .name(name)
                .workspace(workspace)
                .owner(user);

        if(path != null)
            builder.path(path);

        return projectRepository.save(builder.build());
    }

    public Project getProjectById(UUID id) {
        //TODO throw custom exception
        return projectRepository.findById(id).orElseThrow(RuntimeException::new);
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

        return projectRepository.save(currentProject);
    }

    public void deleteProject(UUID id) {
        Project project = this.getProjectById(id);
        projectRepository.delete(project);
    }

    public Project getProjectWithNewSeq(UUID id) {
        //TODO throw custom exception
        Project project = projectRepository.findByIdForUpdate(id).orElseThrow(RuntimeException::new);
        return projectRepository.save(project.incrementSeq());
    }
}
