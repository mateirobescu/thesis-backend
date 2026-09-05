package com.mateirobescu.thesis.files;

import java.util.UUID;

public record FilePatchRequest (
        UUID projectId,
        String filename,
        String path,
        UUID ownerId
) {}