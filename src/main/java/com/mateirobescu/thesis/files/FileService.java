package com.mateirobescu.thesis.files;

import com.mateirobescu.thesis.exception.NotFoundException;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class FileService {

    FileRepository fileRepository;
    ProjectService projectService;
    UserService userService;

    public FileService(FileRepository fileRepository, ProjectService projectService, UserService userService) {
        this.fileRepository = fileRepository;
        this.projectService = projectService;
        this.userService = userService;
    }

    public File createFile(FileCreateCommand command) {
        User user = userService.getUserById(command.ownerId());
        Project project = projectService.getProjectById(command.projectId());
        File file = File.builder()
                .project(project)
                .path(command.path())
                .owner(user)
                .build();

        File savedFile = fileRepository.save(file);
        log.info("File created id={}", savedFile.getId());
        return savedFile;
    }

    public File getFileById(UUID projectId, UUID id) {
        return fileRepository.findByProject_IdAndIdAndDeletedAtIsNull(projectId, id).orElseThrow(() -> new NotFoundException("File", id));
    }

    //TODO change the error to make it more explicit, not just path is ambiguous
    public File getFileByPath(UUID projectId, String path) {
        return fileRepository.findByProject_IdAndPathAndDeletedAtIsNull(projectId, path).orElseThrow(() -> new NotFoundException("File", path));
    }

    public List<File> getFilesByProject(UUID projectId) {
        Project project = projectService.getProjectById(projectId);
        return fileRepository.findByProjectAndDeletedAtIsNull(project);
    }

    public File patchFile(UUID projectId,UUID id, FilePatchRequest request) {
        File currentFile = this.getFileById(projectId, id);
        User newOwner = request.ownerId() != null ? userService.getUserById(request.ownerId()) : null;

        currentFile.applyPatch(new FilePatch(request.path(), newOwner));

        File savedFile = fileRepository.save(currentFile);
        log.info("File patched id={}", currentFile.getId());
        return savedFile;
    }

    public void markAsDeleted(UUID projectId, UUID id) {
        File file = this.getFileById(projectId, id).markAsDeleted();
        fileRepository.save(file);
        log.info("File marked as deleted id={}", file.getId());
    }

}
