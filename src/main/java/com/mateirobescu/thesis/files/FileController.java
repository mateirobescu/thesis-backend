package com.mateirobescu.thesis.files;

import com.mateirobescu.thesis.files.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/projects/{projectId}/files")
class FileController {
    FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    //TODO to deprecate
    @PostMapping
    public FileResponse createFile(@PathVariable UUID projectId, @Valid @RequestBody FileCreateRequest request) {
        return FileResponse.fromFile(fileService.createFile(FileCreateCommand.fromRequest(projectId, request)));
    }

    @GetMapping("/{fileId}")
    public FileResponse getFile(@PathVariable UUID projectId, @PathVariable UUID fileId) {
        File file = fileService.getFileById(projectId, fileId);
        return FileResponse.fromFile(file);
    }

    @GetMapping("/by-path")
    public FileResponse getFileByPath(
            @PathVariable UUID projectId,
            @RequestParam String path
    ) {
        File file = fileService.getFileByPath(projectId, path);
        return FileResponse.fromFile(file);
    }

    @GetMapping
    public FileListResponse getFiles(@PathVariable UUID projectId) {
        return FileListResponse.fromFiles(
                fileService.getFilesByProject(projectId)
        );
    }

    @PatchMapping("/{fileId}")
    public FileResponse patchFile(
            @PathVariable UUID projectId,
            @PathVariable UUID fileId,
            @Valid @RequestBody FilePatchRequest request
    ) {
        //TODO don't like the request being passed directly
        return FileResponse.fromFile(fileService.patchFile(projectId, fileId, request));
    }

    //TODO maybe handle this differently?
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID projectId, @PathVariable UUID fileId) {
        fileService.markAsDeleted(projectId, fileId);
        return ResponseEntity.noContent().build();
    }
}
