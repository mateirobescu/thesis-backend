package com.mateirobescu.thesis.operations;

import com.mateirobescu.thesis.exception.InvalidPayloadException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turns the raw JSON payload of an operation into the class its {@link OperationType} requires and validates it.
 */
@Component
class OperationPayloadParser {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    OperationPayloadParser(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    /**
     * Parses and validates a payload.
     *
     * @throws InvalidPayloadException if the type is not accepted yet, the JSON cannot be read as the type's payload
     *                                 class, or the resulting payload fails validation
     */
    public OperationPayload parse(OperationType type, JsonNode payload) {
        Class<? extends OperationPayload> payloadClass = type.payloadClass();
        if (payloadClass == null) {
            throw new InvalidPayloadException(
                    "Operation type " + type + " is not supported",
                    Map.of("type", List.of("Operation type " + type + " is not supported"))
            );
        }

        OperationPayload parsed;
        try {
            parsed = objectMapper.treeToValue(payload, payloadClass);
        } catch (JacksonException e) {
            throw new InvalidPayloadException(
                    "Payload cannot be read as a " + type + " operation",
                    Map.of("payload", List.of(e.getOriginalMessage()))
            );
        }

        if (parsed == null) {
            throw new InvalidPayloadException(
                    "Payload is required",
                    Map.of("payload", List.of("must not be null"))
            );
        }

        Set<ConstraintViolation<OperationPayload>> violations = validator.validate(parsed);
        if (!violations.isEmpty()) {
            Map<String, List<String>> errors = violations.stream().collect(
                    Collectors.groupingBy(
                            violation -> "payload." + violation.getPropertyPath(),
                            Collectors.mapping(ConstraintViolation::getMessage, Collectors.toList())
                    )
            );
            throw new InvalidPayloadException("Payload is not a valid " + type + " operation", errors);
        }

        return parsed;
    }
}
