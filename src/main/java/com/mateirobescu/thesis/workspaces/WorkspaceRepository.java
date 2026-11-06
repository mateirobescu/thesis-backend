package com.mateirobescu.thesis.workspaces;

import com.mateirobescu.thesis.users.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    /**
     * Finds a workspace that has not been soft-deleted.
     */
    Optional<Workspace> findByIdAndDeletedAtIsNull(UUID id);

    /**
     * Lists the workspaces owned by a user that are not soft-deleted.
     */
    List<Workspace> findByOwnerAndDeletedAtIsNull(User owner);
}
