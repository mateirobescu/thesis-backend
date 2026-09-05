package com.mateirobescu.thesis.projects;

import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.workspaces.Workspace;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.util.UUID;

public record ProjectPatch (
        Workspace workspace,
        String name,
        String path,
        User owner
) { }
