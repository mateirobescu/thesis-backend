package com.mateirobescu.thesis.files;

import com.mateirobescu.thesis.exception.ConflictException;
import com.mateirobescu.thesis.exception.ConstraintViolations;
import com.mateirobescu.thesis.exception.NotFoundException;
import com.mateirobescu.thesis.projects.Project;
import com.mateirobescu.thesis.projects.ProjectService;
import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.users.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Files inside projects: creation, lookup, patching and soft deletion. Within a project a file is identified by
 * its path.
 */
@Service
@Slf4j
public class FileService {

    private final FileRepository fileRepository;
    private final ProjectService projectService;
    private final UserService userService;

    public FileService(FileRepository fileRepository, ProjectService projectService, UserService userService) {
        this.fileRepository = fileRepository;
        this.projectService = projectService;
        this.userService = userService;
    }

    /**
     * Creates a file at the command's path in a project.
     *
     * @return the saved file
     * @throws ConflictException with field {@code path} if a file that is not soft-deleted already has that path
     *                           in the project, whether found by the pre-check or reported by the unique index
     *                           when two creates race
     * @throws NotFoundException if the owner is not an active, verified user or the project does not exist
     */
    public File createFile(FileCreateCommand command) {
        User user = userService.getUserById(command.ownerId());
        Project project = projectService.getProjectById(command.projectId());

        if(findFileByPath(project.getId(), command.path()).isPresent()) {
            //TODO maybe better error message
            throw new ConflictException("path", "File already exists");
        }

        File file = File.builder()
                .project(project)
                .path(command.path())
                .owner(user)
                .build();

        try {
            File savedFile = fileRepository.save(file);
            log.info("File created id={}", savedFile.getId());
            return savedFile;
        } catch (DataIntegrityViolationException exception) {
            if (ConstraintViolations.isViolationOf(exception, FileConstraints.PROJECT_PATH_UNIQUE_CONSTRAINT)) {
                log.info("Constraint violation - file create failed, a file at this path already exists: {} - {}",
                        project.getId(), file.getPath());
                throw new ConflictException("path", "File already exists");
            }
            throw exception;
        }
    }

    //TODO maybe needing the project too is kind of useless?
    /**
     * Returns a file of the given project that has not been soft-deleted.
     *
     * @throws NotFoundException if the project has no such file, including when it is soft-deleted
     */
    public File getFileById(UUID projectId, UUID id) {
        return fileRepository.findByProject_IdAndIdAndDeletedAtIsNull(projectId, id).orElseThrow(() -> new NotFoundException("File", id));
    }

    /**
     * Looks up a file of a project by its exact path, skipping soft-deleted files.
     *
     * @return the file, or empty if there is none
     */
    public Optional<File> findFileByPath(UUID projectId, String path) {
        return fileRepository.findByProject_IdAndPathAndDeletedAtIsNull(projectId, path);
    }

    //TODO change the error to make it more explicit, not just path is ambiguous
    /**
     * Same lookup as {@link #findFileByPath(UUID, String)}, but for callers that require the file to exist.
     *
     * @throws NotFoundException if the project has no such file that is not soft-deleted
     */
    public File getFileByPath(UUID projectId, String path) {
        return this.findFileByPath(projectId, path).orElseThrow(() -> new NotFoundException("File", path));
    }

    /**
     * Returns the files of a project that are not soft-deleted.
     *
     * @throws NotFoundException if the project does not exist
     */
    public List<File> getFilesByProject(UUID projectId) {
        Project project = projectService.getProjectById(projectId);
        return fileRepository.findByProjectAndDeletedAtIsNull(project);
    }

    /**
     * Changes the path and/or the owner of a file of the given project; {@code null} fields in the command are
     * left as they are. The path is checked for a clash only when it actually changes.
     *
     * @return the saved file
     * @throws NotFoundException if the project has no such file, or the new owner is not an active, verified user
     * @throws ConflictException with field {@code path} if another file of the project that is not soft-deleted
     *                           already has the new path, whether found by the pre-check or reported by the
     *                           unique index when two requests race
     */
    public File patchFile(UUID projectId, UUID id, FilePatchCommand command) {
        File file = this.getFileById(projectId, id);
        User newOwner = command.ownerId() != null ? userService.getUserById(command.ownerId()) : null;

        if(command.path() != null && !command.path().equals(file.getPath())
                && this.findFileByPath(projectId, command.path()).isPresent()) {
            //TODO maybe better error message
            throw new ConflictException("path", "File already exists");
        }

        file.applyPatch(new FilePatch(command.path(), newOwner));

        try {
            File savedFile = fileRepository.save(file);
            log.info("File patched id={}", file.getId());
            return savedFile;
        } catch (DataIntegrityViolationException exception) {
            if (ConstraintViolations.isViolationOf(exception, FileConstraints.PROJECT_PATH_UNIQUE_CONSTRAINT)) {
                log.info("Constraint violation - file patch failed, a file at this path already exists: {} - {}",
                        projectId, file.getPath());
                throw new ConflictException("path", "File already exists");
            }
            throw exception;
        }
    }

    /**
     * Soft-deletes a file of the given project by setting {@code deletedAt}; the row is kept.
     *
     * @throws NotFoundException if the project has no such file
     */
    public void deleteFile(UUID projectId, UUID id) {
        File fileToDelete = this.getFileById(projectId, id);
        fileToDelete.markDeleted();
        fileRepository.save(fileToDelete);
        log.info("File marked deleted id={}", id);
    }

}
