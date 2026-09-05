package com.mateirobescu.thesis.files;

import com.mateirobescu.thesis.files.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/files")
class FileController {
    FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping
    public FileResponse createFile(@Valid @RequestBody FileCreateRequest request) {
        return FileResponse.fromFile(fileService.createFile(request.filename(), request.path(), request.projectId(), request.ownerId()));
    }

    //    @GetMapping
//    public UserListResponse getAllUsers() {
//        return UserListResponse.fromUsers(userService.getAllUsers());
//    }
//
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

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID fileId) {
        fileService.deleteFile(fileId);
        return ResponseEntity.noContent().build();
    }
}
