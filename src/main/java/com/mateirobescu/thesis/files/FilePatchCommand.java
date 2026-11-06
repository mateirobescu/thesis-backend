package com.mateirobescu.thesis.files;

import java.util.UUID;

/**
 * Fields to change on a file. A {@code null} field means "leave unchanged".
 */
public record FilePatchCommand(
        String path,
        UUID ownerId
) {
    public static FilePatchCommand fromRequest(FilePatchRequest request) {
        return new FilePatchCommand(request.path(), request.ownerId());
    }
}
