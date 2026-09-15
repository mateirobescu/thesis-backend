package com.mateirobescu.thesis.files;

import java.util.UUID;

public record FileResolveResponse(
        FileResponse file,
        Boolean created
) {
    public static FileResolveResponse fromResult(FileResolveResult result) {
        return new FileResolveResponse(FileResponse.fromFile(result.file()), result.created());
    }
}
