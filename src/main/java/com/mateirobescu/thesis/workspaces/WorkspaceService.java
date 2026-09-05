package com.mateirobescu.thesis.workspaces;

import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.users.UserPatchRequest;
import com.mateirobescu.thesis.users.UserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class WorkspaceService {

    WorkspaceRepository workspaceRepository;
    UserService userService;

    public WorkspaceService(WorkspaceRepository workspaceRepository, UserService userService) {
        this.workspaceRepository = workspaceRepository;
        this.userService = userService;
    }

    public Workspace createWorkspace(String name, UUID userId) {
        User user = userService.getUserById(userId);
        Workspace workspace = Workspace.builder()
                .name(name)
                .owner(user)
                .build();

        return workspaceRepository.save(workspace);
    }

    public Workspace getWorkspaceById(UUID id) {
        //TODO throw custom exception
        return workspaceRepository.findById(id).orElseThrow(RuntimeException::new);
    }

    public List<Workspace> getWorkspacesByUser(UUID userId) {
        User owner = userService.getUserById(userId);
        return this.getWorkspacesByUser(owner);
    }

    public List<Workspace> getWorkspacesByUser(User owner) {
        return workspaceRepository.findByOwner(owner);
    }

//    public List<User> getAllUsers() {
//        return userRepository.findAll();
//    }
//
    public Workspace patchWorkspace(UUID id, WorkspacePatchRequest request) {
        Workspace currentWorkspace = this.getWorkspaceById(id);
        User newOwner = request.ownerId() != null ? userService.getUserById(request.ownerId()) : null;

        currentWorkspace.applyPatch(new WorkspacePatch(request.name(), newOwner));

        return workspaceRepository.save(currentWorkspace);
    }
//
    public void deleteWorkspace(UUID id) {
        Workspace currentWorkspace = this.getWorkspaceById(id);
        workspaceRepository.delete(currentWorkspace);
    }
}
