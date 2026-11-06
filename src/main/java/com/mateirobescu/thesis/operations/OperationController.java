package com.mateirobescu.thesis.operations;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/projects/{projectId}/operations")
class OperationController {

    OperationService operationService;

    public OperationController(OperationService operationService) {
        this.operationService = operationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OperationResponse createOperation(
            @PathVariable("projectId") UUID projectId,
            @Valid @RequestBody OperationCreateRequest request
    ) {

        Operation operation = operationService.createOperation(
                OperationCreateCommand.fromRequest(projectId, request)
        );

        return OperationResponse.fromOperation(
            operation
        );
    }

    @GetMapping("/{seq}")
    public OperationResponse getOperationBySeq(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("seq") Long seq
    ) {
        return OperationResponse.fromOperation(
                operationService.getOperationBySeq(projectId, seq)
        );
    }

    @GetMapping
    public OperationListResponse getOperationsWithSeqGreaterThan(
            @PathVariable("projectId") UUID projectId,
            @RequestParam("seqGreaterThan") Long seq
    ) {
        return OperationListResponse.fromOperations(
                operationService.getProjectOperationsWithSeqGreaterThan(projectId, seq)
        );
    }
}
