package com.mateirobescu.thesis.files;

import com.mateirobescu.thesis.projects.Project;
import com.mateirobescu.thesis.projects.ProjectResponse;

import java.time.Instant;
import java.util.UUID;

public record FileResponse(
        UUID id,
        UUID projectId,
        String path,
        Long seq,
        UUID ownerId,
        Instant createdAt
) {
    public static FileResponse fromFile(File file) {
        return new FileResponse(
                file.getId(),
                file.getProject().getId(),
                file.getPath(),
                file.getSeq(),
                file.getOwner().getId(),
                file.getCreatedAt()
        );
    }
}
