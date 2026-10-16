package top.egon.cola.archetype.source.light.adapter.teaching.pojo.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public record ScheduleCourseDTO(
        @NotNull @JsonDeserialize(using = UtcInstantInputDeserializer.class)
        @Schema(type = "string", format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "2026-10-13T06:07:08.123456Z",
                description = "Offset or Z datetime; normalized to microseconds before validation and persistence.") Instant startsAt,
        @NotNull @JsonDeserialize(using = UtcInstantInputDeserializer.class)
        @Schema(type = "string", format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "2026-10-13T07:07:08.123456Z",
                description = "Offset or Z datetime; must be after startsAt at microsecond precision.") Instant endsAt) {

    /** Numeric Jackson timestamp coercion is not part of the schedule HTTP contract. */
    public static final class UtcInstantInputDeserializer extends StdScalarDeserializer<Instant> {
        private static final Pattern OFFSET_DATETIME = Pattern.compile(
                "[0-9]{4}-[0-9]{2}-[0-9]{2}[Tt][0-9]{2}:[0-9]{2}:[0-5][0-9]"
                        + "(?:\\.[0-9]{1,9})?(?:[Zz]|[+-][0-9]{2}:[0-9]{2})");

        public UtcInstantInputDeserializer() {
            super(Instant.class);
        }

        @Override
        public Instant deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (parser.currentToken() != JsonToken.VALUE_STRING) {
                return (Instant) context.handleUnexpectedToken(Instant.class, parser);
            }
            String value = parser.getText();
            if (!OFFSET_DATETIME.matcher(value).matches()) {
                return (Instant) context.handleWeirdStringValue(Instant.class, value, "offset or Z datetime required");
            }
            try {
                return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
            } catch (DateTimeParseException exception) {
                return (Instant) context.handleWeirdStringValue(Instant.class, value, "invalid offset datetime");
            }
        }
    }
}
