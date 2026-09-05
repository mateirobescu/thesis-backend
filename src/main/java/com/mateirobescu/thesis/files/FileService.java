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
import org.springframework.stereotype.Service;

import java.util.List;
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

    public File createFile(String name, String path, UUID projectId, UUID userId) {
        User user = userService.getUserById(userId);
        Project project = projectService.getProjectById(projectId);
        var builder = File.builder()
                .filename(name)
                .project(project)
                .owner(user);

        if(path != null)
            builder.path(path);

        return fileRepository.save(builder.build());
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

        currentFile.applyPatch(new FilePatch(newproject, request.filename(), request.path(), newOwner));

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
}
