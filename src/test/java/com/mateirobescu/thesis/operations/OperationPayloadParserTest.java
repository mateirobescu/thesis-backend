package com.mateirobescu.thesis.operations;

import com.mateirobescu.thesis.exception.InvalidPayloadException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.instancio.Instancio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Uses a real {@link ObjectMapper} and {@link Validator}: the parser is only glue between them, so mocking both
 * would leave nothing to test.
 */
class OperationPayloadParserTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final OperationPayloadParser parser = new OperationPayloadParser(objectMapper, validator);

    private ObjectNode validFilePayload() {
        return objectMapper.valueToTree(Instancio.create(FileOperationPayload.class));
    }

    @Nested
    class Parse {

        @Test
        void should_returnTypedPayload_whenPayloadMatchesType() {
            FileOperationPayload expected = Instancio.create(FileOperationPayload.class);

            OperationPayload parsed = parser.parse(OperationType.FILE, objectMapper.valueToTree(expected));

            assertEquals(expected, parsed);
        }

        @Test
        void should_rejectType_whenTypeHasNoPayloadClass() {
            JsonNode payload = validFilePayload();

            assertThatThrownBy(() -> parser.parse(OperationType.TERMINAL, payload))
                    .isInstanceOfSatisfying(InvalidPayloadException.class, e ->
                            assertThat(e.getErrors()).containsOnlyKeys("type"));
        }

        @Test
        void should_reportMissingField_whenRequiredFieldIsAbsent() {
            ObjectNode payload = validFilePayload();
            payload.remove("chars");

            assertThatThrownBy(() -> parser.parse(OperationType.FILE, payload))
                    .isInstanceOfSatisfying(InvalidPayloadException.class, e ->
                            assertThat(e.getErrors()).containsOnlyKeys("payload.chars"));
        }

        @Test
        void should_reportEveryMissingField_whenPayloadIsEmpty() {
            JsonNode payload = objectMapper.createObjectNode();

            assertThatThrownBy(() -> parser.parse(OperationType.FILE, payload))
                    .isInstanceOfSatisfying(InvalidPayloadException.class, e ->
                            assertThat(e.getErrors()).containsOnlyKeys("payload.fileId", "payload.charOffset", "payload.length", "payload.chars"));
        }

        @Test
        void should_rejectPayload_whenFieldHasWrongType() {
            ObjectNode payload = validFilePayload();
            payload.put("fileId", "not-a-uuid");

            assertThatThrownBy(() -> parser.parse(OperationType.FILE, payload))
                    .isInstanceOfSatisfying(InvalidPayloadException.class, e ->
                            assertThat(e.getErrors()).containsOnlyKeys("payload"));
        }

        @Test
        void should_rejectPayload_whenPayloadIsJsonNull() {
            JsonNode payload = objectMapper.nullNode();

            assertThatThrownBy(() -> parser.parse(OperationType.FILE, payload))
                    .isInstanceOfSatisfying(InvalidPayloadException.class, e ->
                            assertThat(e.getErrors()).containsOnlyKeys("payload"));
        }
    }
}
