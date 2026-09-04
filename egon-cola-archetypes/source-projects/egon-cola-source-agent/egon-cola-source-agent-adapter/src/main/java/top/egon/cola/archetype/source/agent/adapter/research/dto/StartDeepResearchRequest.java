package top.egon.cola.archetype.source.agent.adapter.research.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import top.egon.cola.archetype.source.agent.domain.research.model.ReportLanguageEnum;

/** External JSON command for the one Deep Research SSE operation. */
@JsonIgnoreProperties(ignoreUnknown = false)
@JsonDeserialize(using = StartDeepResearchRequest.Deserializer.class)
@Schema(name = "StartDeepResearchRequest")
public record StartDeepResearchRequest(
        @NotBlank @Size(min = 3, max = 500)
        @Schema(example = "agent architecture") String topic,
        @Schema(allowableValues = {"ZH_CN", "EN_US"}, example = "ZH_CN") ReportLanguageEnum reportLanguage,
        @Min(3) @Max(20) @Schema(example = "8", minimum = "3", maximum = "20") Integer maxSources) {

    public StartDeepResearchRequest {
        topic = topic == null ? null : topic.trim();
    }

    /** Jackson-only boundary parser that distinguishes absent optional fields from explicit null. */
    public static final class Deserializer extends StdDeserializer<StartDeepResearchRequest> {

        private static final java.util.Set<String> FIELDS = java.util.Set.of("topic", "reportLanguage", "maxSources");

        public Deserializer() {
            super(StartDeepResearchRequest.class);
        }

        @Override
        public StartDeepResearchRequest deserialize(JsonParser parser,
                                                     com.fasterxml.jackson.databind.DeserializationContext context)
                throws java.io.IOException {
            ObjectCodec codec = parser.getCodec();
            JsonNode root = codec.readTree(parser);
            if (root == null || !root.isObject()) {
                throw com.fasterxml.jackson.databind.JsonMappingException.from(parser,
                        "research request must be a JSON object");
            }
            java.util.Iterator<String> names = root.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                if (!FIELDS.contains(name)) {
                    throw context.weirdStringException(name, StartDeepResearchRequest.class,
                            "unknown research request property");
                }
            }
            JsonNode topic = root.get("topic");
            JsonNode language = root.get("reportLanguage");
            JsonNode maxSources = root.get("maxSources");
            if (language != null && language.isNull()) {
                throw com.fasterxml.jackson.databind.JsonMappingException.from(parser,
                        "reportLanguage must not be null");
            }
            if (maxSources != null && maxSources.isNull()) {
                throw com.fasterxml.jackson.databind.JsonMappingException.from(parser,
                        "maxSources must not be null");
            }
            if (topic != null && !topic.isTextual()) {
                throw com.fasterxml.jackson.databind.JsonMappingException.from(parser,
                        "topic must be a JSON string");
            }
            ReportLanguageEnum parsedLanguage = language == null ? null : parseLanguage(language, context);
            Integer parsedMaxSources = maxSources == null ? null : parseMaxSources(maxSources, context);
            return new StartDeepResearchRequest(topic == null ? null : topic.textValue(),
                    parsedLanguage, parsedMaxSources);
        }

        private static ReportLanguageEnum parseLanguage(JsonNode value,
                                                        com.fasterxml.jackson.databind.DeserializationContext context)
                throws java.io.IOException {
            if (!value.isTextual()) {
                throw com.fasterxml.jackson.databind.JsonMappingException.from(context.getParser(),
                        "reportLanguage must be a JSON string");
            }
            try {
                return ReportLanguageEnum.valueOf(value.textValue());
            } catch (IllegalArgumentException failure) {
                throw context.weirdStringException(value.textValue(), ReportLanguageEnum.class,
                        "unsupported reportLanguage");
            }
        }

        private static Integer parseMaxSources(JsonNode value,
                                               com.fasterxml.jackson.databind.DeserializationContext context)
                throws java.io.IOException {
            if (!value.isIntegralNumber() || !value.canConvertToInt()) {
                throw com.fasterxml.jackson.databind.JsonMappingException.from(context.getParser(),
                        "maxSources must be an integer");
            }
            return value.intValue();
        }
    }
}
