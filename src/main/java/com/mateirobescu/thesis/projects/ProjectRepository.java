package com.mateirobescu.thesis.projects;

import com.mateirobescu.thesis.workspaces.Workspace;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ProjectRepository extends JpaRepository<Project, UUID> {
    
    /**
     * Finds a project that has not been soft-deleted.
     */
    Optional<Project> findByIdAndDeletedAtIsNull(UUID id);

    /**
     * Lists the projects of a workspace that are not soft-deleted.
     */
    List<Project> findByWorkspaceAndDeletedAtIsNull(Workspace workspace);

    /**
     * Loads a project that is not soft-deleted with a pessimistic write lock ({@code SELECT ... FOR UPDATE}). It must run inside
     * a transaction, and the lock is held until that transaction ends.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Project p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Project> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Tells whether a project (not soft-deleted) with this path and name exists in the workspace.
     */
    boolean existsByWorkspace_IdAndPathAndNameAndDeletedAtIsNull(UUID workspaceId, String path, String name);

}
