package com.mateirobescu.thesis.operations;

/**
 * The kind of an operation. Each type names the class its payload is parsed into; a {@code null} class means the
 * type is not accepted yet.
 */
public enum OperationType {
    FILE(FileOperationPayload.class),
    TERMINAL(null);

    private final Class<? extends OperationPayload> payloadClass;

    OperationType(Class<? extends OperationPayload> payloadClass) {
        this.payloadClass = payloadClass;
    }

    public Class<? extends OperationPayload> payloadClass() {
        return payloadClass;
    }
}
