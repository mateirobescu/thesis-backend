package com.mateirobescu.thesis.files;

import com.mateirobescu.thesis.files.File;
import com.mateirobescu.thesis.files.FilePatch;
import com.mateirobescu.thesis.files.FilePatchRequest;
import com.mateirobescu.thesis.files.FileRepository;
import com.mateirobescu.thesis.projects.Project;
import com.mateirobescu.thesis.projects.ProjectService;
import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.users.UserService;
import com.mateirobescu.thesis.projects.Project;
import com.mateirobescu.thesis.projects.ProjectService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileService {

    FileRepository fileRepository;
    ProjectService projectService;
    UserService userService;

    public FileService(FileRepository fileRepository, ProjectService projectService, UserService userService) {
        this.fileRepository = fileRepository;
        this.projectService = projectService;
        this.userService = userService;
    }

    public File createFile(String path, UUID projectId, UUID userId) {
        User user = userService.getUserById(userId);
        Project project = projectService.getProjectById(projectId);
        File file = File.builder()
                .project(project)
                .path(path)
                .owner(user)
                .build();

        return fileRepository.save(file);
    }

    public File getFileById(UUID id) {
        //TODO throw custom exception
        return fileRepository.findById(id).orElseThrow(RuntimeException::new);
    }

    public List<File> getFilesByProject(UUID projectId) {
        Project project = projectService.getProjectById(projectId);
        return fileRepository.findByProject(project);
    }

    public File patchFile(UUID id, FilePatchRequest request) {
        File currentFile = this.getFileById(id);
        User newOwner = request.ownerId() != null ? userService.getUserById(request.ownerId()) : null;
        Project newproject = request.projectId() != null ? projectService.getProjectById(request.projectId()) : null;

        currentFile.applyPatch(new FilePatch(newproject, request.path(), newOwner));

        return fileRepository.save(currentFile);
    }

    public void deleteFile(UUID id) {
        File File = this.getFileById(id);
        fileRepository.delete(File);
    }

    public File getFileWithNewSeq(UUID id) {
        //TODO throw custom exception
        File file = fileRepository.findByIdForUpdate(id).orElseThrow(RuntimeException::new);
        return fileRepository.save(file.incrementSeq());
    }

    public FileResolveResult resolveOrCreateFile(UUID projectId, UUID userId, String fullPath) {
        Optional<File> existing = fileRepository.findByProject_IdAndPath(projectId, fullPath);

        System.err.println("CALLED");

        if (existing.isPresent())
            return new FileResolveResult(existing.get(), false);

        try {
            File created = this.createFile(fullPath, projectId, userId);
            return new FileResolveResult(created, true);
        } catch (DataIntegrityViolationException e) {
            // just fetch it again, as it should exist
            return fileRepository.findByProject_IdAndPath(projectId, fullPath)
                    .map(file -> new FileResolveResult(file, false))
                    .orElseThrow(() -> e);
        }
    }

}
