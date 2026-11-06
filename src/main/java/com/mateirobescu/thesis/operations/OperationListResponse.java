package com.mateirobescu.thesis.operations;

import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.users.UserListResponse;
import com.mateirobescu.thesis.users.UserResponse;

import java.util.Arrays;
import java.util.List;

public record OperationListResponse (
        List<OperationResponse> operations
) {
    public static OperationListResponse fromOperations(Operation... operation) {
        return OperationListResponse.fromOperations(Arrays.stream(operation).toList());
    }

    public static OperationListResponse fromOperations(List<Operation> operations) {
        return new OperationListResponse(operations.stream()
                .map(OperationResponse::fromOperation)
                .toList()
        );
    }
}