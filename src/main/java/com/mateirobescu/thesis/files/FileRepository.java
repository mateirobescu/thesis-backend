package com.mateirobescu.thesis.files;

import com.mateirobescu.thesis.projects.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<File, UUID> {
    
    List<File> findByProjectAndDeletedAtIsNull(Project project);

    Optional<File> findByProject_IdAndPathAndDeletedAtIsNull(UUID project_id, String path);

    Optional<File> findByProject_IdAndIdAndDeletedAtIsNull(UUID project_id, UUID id);

}
