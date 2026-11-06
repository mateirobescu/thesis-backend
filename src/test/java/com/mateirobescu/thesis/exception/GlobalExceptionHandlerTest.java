package com.mateirobescu.thesis.exception;

import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks that {@link GlobalExceptionHandler} turns each exception into the right status and {@link ErrorResponse}.
 * It uses MockMvc without a Spring context, against a small controller whose endpoints just throw.
 */
class GlobalExceptionHandlerTest {

    private static final UUID ID = UUID.fromString("0198f3a2-7c1b-7d4e-9a53-2b8e6f1c4d70");

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @RestController
    static class ThrowingController {

        public record Body(@NotBlank String name) { }

        @GetMapping("/not-found")
        void notFound() {
            throw new NotFoundException("User", ID);
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new ConflictException("email", "An account with this email already exists");
        }

        @GetMapping("/conflict-without-field")
        void conflictWithoutField() {
            throw new ConflictException("The resource is in a conflicting state");
        }

        @GetMapping("/optimistic-lock")
        void optimisticLock() {
            throw new ObjectOptimisticLockingFailureException(Object.class, ID);
        }

        @GetMapping("/invalid-payload")
        void invalidPayload() {
            throw new InvalidPayloadException("Payload is not valid", Map.of("payload.chars", List.of("must not be null")));
        }

        @GetMapping("/unauthenticated")
        void unauthenticated() {
            throw new AuthenticationException("No authenticated user found");
        }

        @GetMapping("/session-expired")
        void sessionExpired() {
            throw new SessionExpiredException();
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("boom");
        }

        @PostMapping("/validated")
        void validated(@Valid @RequestBody Body body) {
        }

        @GetMapping("/by-id/{id}")
        void byId(@PathVariable UUID id) {
        }

        @GetMapping("/needs-param")
        void needsParam(@RequestParam Long seq) {
        }
    }

    @Nested
    class HandledExceptions {

        @Test
        void should_return404_whenResourceIsNotFound() throws Exception {
            mockMvc.perform(get("/not-found"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("User not found: " + ID))
                    .andExpect(jsonPath("$.path").value("/not-found"));
        }

        @Test
        void should_return409WithFieldErrors_whenConflictNamesAField() throws Exception {
            mockMvc.perform(get("/conflict"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.code").value("CONFLICT"))
                    .andExpect(jsonPath("$.errors.email[0]").value("An account with this email already exists"));
        }

        @Test
        void should_return409WithoutFieldErrors_whenConflictNamesNoField() throws Exception {
            mockMvc.perform(get("/conflict-without-field"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"))
                    .andExpect(jsonPath("$.message").value("The resource is in a conflicting state"))
                    .andExpect(jsonPath("$.errors").doesNotExist());
        }

        @Test
        void should_return409_whenAnotherRequestModifiedTheResource() throws Exception {
            mockMvc.perform(get("/optimistic-lock"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.code").value("CONFLICT"))
                    .andExpect(jsonPath("$.message").value("The resource was modified by another request. Please retry."));
        }

        @Test
        void should_return400WithPayloadErrors_whenPayloadIsInvalid() throws Exception {
            mockMvc.perform(get("/invalid-payload"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors['payload.chars'][0]").value("must not be null"));
        }

        @Test
        void should_return400WithFieldErrors_whenRequestBodyFailsValidation() throws Exception {
            mockMvc.perform(post("/validated").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors.name").isNotEmpty());
        }

        @Test
        void should_return400_whenRequestBodyIsMalformed() throws Exception {
            mockMvc.perform(post("/validated").contentType(MediaType.APPLICATION_JSON).content("not json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST_BODY"));
        }

        @Test
        void should_return404_whenNoEndpointMatches() throws Exception {
            mockMvc.perform(get("/no-such-endpoint"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("INVALID_ENDPOINT"));
        }

        @Test
        void should_return405_whenMethodIsNotSupported() throws Exception {
            mockMvc.perform(post("/not-found"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
        }

        @Test
        void should_return400_whenPathVariableHasTheWrongType() throws Exception {
            mockMvc.perform(get("/by-id/not-a-uuid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"))
                    .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id': expected UUID"));
        }

        @Test
        void should_return400_whenRequiredQueryParameterIsMissing() throws Exception {
            mockMvc.perform(get("/needs-param"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.message").value(containsString("seq")));
        }

        @Test
        void should_return415_whenContentTypeIsNotSupported() throws Exception {
            mockMvc.perform(post("/validated").contentType(MediaType.TEXT_PLAIN).content("{\"name\": \"John\"}"))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
        }

        @Test
        void should_return401_whenAuthenticationFails() throws Exception {
            mockMvc.perform(get("/unauthenticated"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.message").value("No authenticated user found"));
        }

        @Test
        void should_return401_whenSessionHasExpired() throws Exception {
            mockMvc.perform(get("/session-expired"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        @Test
        void should_return500WithoutTheCause_whenAnUnexpectedErrorIsThrown() throws Exception {
            mockMvc.perform(get("/boom"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
        }
    }
}
