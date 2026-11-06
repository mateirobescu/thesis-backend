package com.mateirobescu.thesis.files;

import com.mateirobescu.thesis.validation.ValidPath;

import java.util.UUID;

public record FilePatchRequest (
        @ValidPath
        String path,
        UUID ownerId
) {}