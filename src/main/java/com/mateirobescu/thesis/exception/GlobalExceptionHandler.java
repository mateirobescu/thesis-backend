package com.mateirobescu.thesis.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e, HttpServletRequest req) {
        log.info("Resource not found: {}", e.getMessage());
        return respond(ErrorResponse.of(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", e.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationFailure(MethodArgumentNotValidException e, HttpServletRequest req) {
        Map<String, List<String>> errors = e.getBindingResult().getFieldErrors().stream()
                        .collect(
                                Collectors.groupingBy(
                                        FieldError::getField,
                                        Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())
                                )
                        );

        log.debug("Validation failed: {}", errors);
        return respond(ErrorResponse.validation(errors, req.getRequestURI()));
    }

    @ExceptionHandler(InvalidPayloadException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPayload(InvalidPayloadException e, HttpServletRequest req) {
        log.debug("Invalid payload: {}", e.getErrors());
        return respond(ErrorResponse.validation(e.getErrors(), req.getRequestURI()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException e, HttpServletRequest req) {
        Map<String, List<String>> fieldErrors =
                e.getField() != null ? Map.of(e.getField(), List.of(e.getMessage())) : null;
        return respond(ErrorResponse.conflict(e.getMessage(), fieldErrors, req.getRequestURI()));
    }

    //TODO maybe should throw a custom error for a custom message
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException e, HttpServletRequest req) {
        log.info("Concurrent modification: {}", e.getMessage());
        return respond(ErrorResponse.conflict("The resource was modified by another request. Please retry.", null, req.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException e, HttpServletRequest req) {
        log.info("Malformed request received: {}", e.getMostSpecificCause().getMessage());
        return respond(ErrorResponse.of(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST_BODY", "Malformed request body: " + e.getMostSpecificCause().getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoEndpointFound(NoHandlerFoundException e, HttpServletRequest req) {
        log.warn("No endpoint found for {} {}", e.getHttpMethod(), e.getRequestURL());
        return respond(ErrorResponse.of(HttpStatus.NOT_FOUND, "INVALID_ENDPOINT", "No such endpoint: " + e.getRequestURL(), req.getRequestURI()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest req) {
        log.info("Method not supported: {}", e.getMessage());
        return respond(ErrorResponse.of(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", e.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> authenticationFailure(AuthenticationException e, HttpServletRequest req) {
        log.warn("Authentication failed: {}", e.getMessage());
        return respond(ErrorResponse.of(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", e.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest req) {
        String message = String.format(
                "Invalid value for parameter '%s': expected %s",
                e.getName(),
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "a different type"
        );
        log.info(message);
        return respond(ErrorResponse.of(HttpStatus.BAD_REQUEST, "TYPE_MISMATCH", message, req.getRequestURI()));
    }

    /**
     * Last resort for anything without a more specific handler. Spring's own exceptions, such as a missing request
     * parameter or an unsupported content type, already know their status, so that status is kept instead of
     * reporting them as server errors. Everything else is a 500 whose body says nothing about the cause, which
     * stays in the log.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e, HttpServletRequest req) {
        if (e instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            String detail = springError.getBody().getDetail();
            log.info("Request rejected with {}: {}", status, e.getMessage());
            return respond(ErrorResponse.of(status, status.name(), detail != null ? detail : status.getReasonPhrase(), req.getRequestURI()));
        }

        log.error("Unhandled exception", e);
        return respond(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", req.getRequestURI()));
    }

    // The HTTP status always comes from the body, so the two cannot disagree.
    private static ResponseEntity<ErrorResponse> respond(ErrorResponse body) {
        return ResponseEntity.status(body.status()).body(body);
    }
}
