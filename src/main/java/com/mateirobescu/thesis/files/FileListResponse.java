package com.mateirobescu.thesis.files;

import java.util.Arrays;
import java.util.List;

public record FileListResponse(
        List<FileResponse> files
) {
    public static FileListResponse fromFiles(File... files) {
        return FileListResponse.fromFiles(Arrays.stream(files).toList());
    }

    public static FileListResponse fromFiles(List<File> files) {
        return new FileListResponse(files.stream()
                .map(FileResponse::fromFile)
                .toList()
        );
    }
}

