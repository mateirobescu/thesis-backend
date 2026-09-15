package com.mateirobescu.thesis.files;

import com.mateirobescu.thesis.files.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("files")
class FileController {
    FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    //TODO to deprecate
    @PostMapping
    public FileResponse createFile(@Valid @RequestBody FileCreateRequest request) {
        return FileResponse.fromFile(fileService.createFile(request.path(), request.projectId(), request.ownerId()));
    }

    @GetMapping("/{fileId}")
    public FileResponse getFile(@PathVariable UUID fileId) {
        File file = fileService.getFileById(fileId);
        return FileResponse.fromFile(file);
    }

    @PatchMapping("/{fileId}")
    public FileResponse patchFile(
            @PathVariable UUID fileId,
            @Valid @RequestBody FilePatchRequest request
    ) {
        //TODO don't like the request being passed directly
        return FileResponse.fromFile(fileService.patchFile(fileId, request));
    }

    //TODO maybe handle this differently?
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID fileId) {
        fileService.deleteFile(fileId);
        return ResponseEntity.noContent().build();
    }

    //TODO maybe move these to another controller?
    //TODO also wrong route I wanted /projects/{projectId}/files/resolve
    @PostMapping("/projects/{projectId}/resolve")
    public ResponseEntity<FileResolveResponse> resolveFile(@PathVariable UUID projectId, @Valid @RequestBody FileResolveRequest request) {
        FileResolveResult result = fileService.resolveOrCreateFile(projectId, request.userId(), request.fullPath());

        return ResponseEntity.status(
                result.created() ? HttpStatus.CREATED : HttpStatus.OK
        ).body(FileResolveResponse.fromResult(result));
    }


}
