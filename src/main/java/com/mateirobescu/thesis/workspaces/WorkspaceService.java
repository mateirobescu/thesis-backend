package com.mateirobescu.thesis.workspaces;

import com.mateirobescu.thesis.exception.NotFoundException;
import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.users.UserPatchRequest;
import com.mateirobescu.thesis.users.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
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

        Workspace savedWorkspace = workspaceRepository.save(workspace);
        log.info("Workspace created id={}", savedWorkspace.getId());
        return savedWorkspace;
    }

    public Workspace getWorkspaceById(UUID id) {
        return workspaceRepository.findById(id).orElseThrow(() -> new NotFoundException("Workspace", id));
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

        Workspace savedWorkspace = workspaceRepository.save(currentWorkspace);
        log.info("Workspace patched id={}", currentWorkspace.getId());
        return savedWorkspace;
    }
//
    public void deleteWorkspace(UUID id) {
        Workspace currentWorkspace = this.getWorkspaceById(id);
        log.info("Workspace deleting id={}", currentWorkspace.getId());
        workspaceRepository.delete(currentWorkspace);
        log.info("Workspace deleted id={}", currentWorkspace.getId());
    }
}
