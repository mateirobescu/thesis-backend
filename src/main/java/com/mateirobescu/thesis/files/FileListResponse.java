package com.mateirobescu.thesis.files;

import com.mateirobescu.thesis.events.Event;
import com.mateirobescu.thesis.events.EventListResponse;
import com.mateirobescu.thesis.events.EventResponse;

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

