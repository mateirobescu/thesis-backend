package com.mateirobescu.thesis.files;

import com.mateirobescu.thesis.projects.Project;
import com.mateirobescu.thesis.projects.ProjectResponse;

import java.time.Instant;
import java.util.UUID;

public record FileResponse(
        UUID id,
        UUID projectId,
        String filename,
        String path,
        UUID ownerId,
        Instant createdAt
) {
    public static FileResponse fromFile(File file) {
        return new FileResponse(
                file.getId(),
                file.getProject().getId(),
                file.getFilename(),
                file.getPath(),
                file.getOwner().getId(),
                file.getCreatedAt()
        );
    }
}
