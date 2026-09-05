package com.mateirobescu.thesis.workspaces;

import com.mateirobescu.thesis.users.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/workspaces")
class WorkspaceController {
    WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    public WorkspaceResponse createWorkspace(@Valid @RequestBody WorkspaceCreateRequest request) {
        return WorkspaceResponse.fromWorkspace(workspaceService.createWorkspace(request.name(), request.ownerId()));
    }

//    @GetMapping
//    public UserListResponse getAllUsers() {
//        return UserListResponse.fromUsers(userService.getAllUsers());
//    }
//
    @GetMapping("/{workspaceId}")
    public WorkspaceResponse getWorkspace(@PathVariable UUID workspaceId) {
        Workspace workspace = workspaceService.getWorkspaceById(workspaceId);
        return WorkspaceResponse.fromWorkspace(workspace);
    }

    @PatchMapping("/{workspaceId}")
    public WorkspaceResponse patchWorkspace(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody WorkspacePatchRequest request
    ) {
        //TODO don't like the request being passed directly
        return WorkspaceResponse.fromWorkspace(workspaceService.patchWorkspace(workspaceId, request));
    }

    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<Void> deleteWorkspace(@PathVariable UUID workspaceId) {
        workspaceService.deleteWorkspace(workspaceId);
        return ResponseEntity.noContent().build();
    }
}
