package com.mateirobescu.thesis.files;

import com.mateirobescu.thesis.projects.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface FileRepository extends JpaRepository<File, UUID> {
    
    /**
     * Lists the files of a project that are not soft-deleted.
     */
    List<File> findByProjectAndDeletedAtIsNull(Project project);

    /**
     * Finds a file of a project by exact path, skipping soft-deleted files.
     */
    Optional<File> findByProject_IdAndPathAndDeletedAtIsNull(UUID project_id, String path);

    /**
     * Finds a file by id, only if it belongs to the given project and has not been soft-deleted.
     */
    Optional<File> findByProject_IdAndIdAndDeletedAtIsNull(UUID project_id, UUID id);

}
