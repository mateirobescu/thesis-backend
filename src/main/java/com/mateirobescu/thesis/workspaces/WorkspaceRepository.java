package com.mateirobescu.thesis.workspaces;

import com.mateirobescu.thesis.users.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    List<Workspace> findByOwner(User owner);
}
