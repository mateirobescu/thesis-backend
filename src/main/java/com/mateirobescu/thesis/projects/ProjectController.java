package com.mateirobescu.thesis.projects;

import com.mateirobescu.thesis.workspaces.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/projects")
class ProjectController {
    ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ProjectResponse createProject(@Valid @RequestBody ProjectCreateRequest request) {
        return ProjectResponse.fromProject(projectService.createProject(request.name(), request.path(), request.workspaceId(), request.ownerId()));
    }

    //    @GetMapping
//    public UserListResponse getAllUsers() {
//        return UserListResponse.fromUsers(userService.getAllUsers());
//    }
//
    @GetMapping("/{projectId}")
    public ProjectResponse getProject(@PathVariable UUID projectId) {
        Project project = projectService.getProjectById(projectId);
        return ProjectResponse.fromProject(project);
    }

    @PatchMapping("/{projectId}")
    public ProjectResponse patchProject(
            @PathVariable UUID projectId,
            @Valid @RequestBody ProjectPatchRequest request
    ) {
        //TODO don't like the request being passed directly
        return ProjectResponse.fromProject(projectService.patchProject(projectId, request));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID projectId) {
        projectService.deleteProject(projectId);
        return ResponseEntity.noContent().build();
    }
}
