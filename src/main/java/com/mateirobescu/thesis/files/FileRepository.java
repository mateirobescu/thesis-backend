package com.mateirobescu.thesis.files;

import com.mateirobescu.thesis.projects.Project;
import com.mateirobescu.thesis.workspaces.Workspace;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<File, UUID> {
    
    List<File> findByProject(Project project);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM File f WHERE f.id = :id")
    Optional<File> findByIdForUpdate(@Param("id") UUID id);

}
