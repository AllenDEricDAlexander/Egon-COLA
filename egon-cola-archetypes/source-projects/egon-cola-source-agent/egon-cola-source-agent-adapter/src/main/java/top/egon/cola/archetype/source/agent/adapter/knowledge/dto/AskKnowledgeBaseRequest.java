package top.egon.cola.archetype.source.agent.adapter.knowledge.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * External JSON question for the retrieval-backed answer stream (Spec B §9.2.12).
 *
 * <p>Like the retrieval body it refuses undeclared properties: the answer is produced from the chunks
 * the use case retrieves, so a request that names a model, a collection or a filter condition is
 * malformed rather than a caller's choice, and the refusal is the only way to say so.
 *
 * <p>The question is the one field the contract rejects control characters in: they carry no meaning
 * in a question and would otherwise travel into a prompt, so the rule is stated here where the
 * rejection can still name the field.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
@JsonDeserialize(using = AskKnowledgeBaseRequest.Deserializer.class)
@Schema(name = "AskKnowledgeBaseRequest")
public record AskKnowledgeBaseRequest(
        @NotBlank @Size(max = 2000) @Pattern(regexp = "[\\P{Cntrl}]*",
                message = "question must not carry a control character")
        @Schema(example = "超时怎么配置？", description = "Trimmed before use")
        String question,
        @Min(1) @Max(50) @Schema(example = "8", minimum = "1", maximum = "50",
                description = "Citation cap, defaulting to the component's default-top-k")
        Integer topK) {

    public AskKnowledgeBaseRequest {
        question = normalize(question);
    }

    /**
     * Jackson-only boundary parser that refuses unknown fields instead of dropping them, and reads
     * each declared field with the type it declares.
     *
     * <p>A class-level {@code @JsonIgnoreProperties(ignoreUnknown = false)} cannot do this: whether an
     * unrecognized property throws is the mapper's {@code FAIL_ON_UNKNOWN_PROPERTIES} feature, and the
     * application mapper leaves it off. The published contract still refuses those fields, so the
     * refusal belongs to the type.
     */
    public static final class Deserializer extends StdDeserializer<AskKnowledgeBaseRequest> {

        private static final Set<String> FIELDS = Set.of("question", "topK");

        public Deserializer() {
            super(AskKnowledgeBaseRequest.class);
        }

        @Override
        public AskKnowledgeBaseRequest deserialize(JsonParser parser, DeserializationContext context)
                throws IOException {
            JsonNode root = parser.getCodec().readTree(parser);
            if (root == null || !root.isObject()) {
                throw JsonMappingException.from(parser, "question request must be a JSON object");
            }
            Iterator<String> names = root.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                if (!FIELDS.contains(name)) {
                    throw context.weirdStringException(name, AskKnowledgeBaseRequest.class,
                            "unknown question request property");
                }
            }
            return new AskKnowledgeBaseRequest(text(root, "question", parser), integer(root, "topK", parser));
        }

        private static String text(JsonNode root, String field, JsonParser parser) throws IOException {
            JsonNode node = root.get(field);
            if (node == null || node.isNull()) {
                return null;
            }
            if (!node.isTextual()) {
                throw JsonMappingException.from(parser, field + " must be a JSON string");
            }
            return node.textValue();
        }

        private static Integer integer(JsonNode root, String field, JsonParser parser) throws IOException {
            JsonNode node = root.get(field);
            if (node == null || node.isNull()) {
                return null;
            }
            if (!node.isIntegralNumber() || !node.canConvertToInt()) {
                throw JsonMappingException.from(parser, field + " must be an integer");
            }
            return node.intValue();
        }
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
