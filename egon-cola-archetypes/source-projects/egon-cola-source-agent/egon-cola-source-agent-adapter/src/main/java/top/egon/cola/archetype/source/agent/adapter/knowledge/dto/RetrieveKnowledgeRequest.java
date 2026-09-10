package top.egon.cola.archetype.source.agent.adapter.knowledge.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * External JSON query for one retrieval debug run (Spec B §9.2.11).
 *
 * <p>It carries the query, the result cap and the score floor and nothing else. The collection, the
 * logical model and every filter condition are derived from the knowledge base by the use case, so a
 * caller cannot reach another base's vectors through the body (§9.2.11 logic 4) — and because that
 * guarantee is what the endpoint's safety rests on, the parser refuses an undeclared property instead
 * of dropping it: a request that names {@code collectionId} is told it is malformed rather than being
 * silently answered as if it had not.
 *
 * <p>{@code topK}'s ceiling is the retrieval maximum the component publishes
 * ({@code egon.cola.component.rag.retrieval.max-top-k}), so a value the vector store would refuse is
 * rejected here, with the field named, before any use case is entered.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
@JsonDeserialize(using = RetrieveKnowledgeRequest.Deserializer.class)
@Schema(name = "RetrieveKnowledgeRequest")
public record RetrieveKnowledgeRequest(
        @NotBlank @Size(max = 2000) @Schema(example = "如何配置超时", description = "Trimmed before use")
        String query,
        @Min(1) @Max(50) @Schema(example = "8", minimum = "1", maximum = "50",
                description = "Result cap, defaulting to the component's default-top-k")
        Integer topK,
        @DecimalMin("0.0") @DecimalMax("1.0") @Schema(example = "0.6", minimum = "0.0", maximum = "1.0",
                description = "Score floor, where an absent value accepts every score")
        Double similarityThreshold) {

    public RetrieveKnowledgeRequest {
        query = normalize(query);
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
    public static final class Deserializer extends StdDeserializer<RetrieveKnowledgeRequest> {

        private static final Set<String> FIELDS = Set.of("query", "topK", "similarityThreshold");

        public Deserializer() {
            super(RetrieveKnowledgeRequest.class);
        }

        @Override
        public RetrieveKnowledgeRequest deserialize(JsonParser parser, DeserializationContext context)
                throws IOException {
            JsonNode root = parser.getCodec().readTree(parser);
            if (root == null || !root.isObject()) {
                throw JsonMappingException.from(parser, "retrieval request must be a JSON object");
            }
            Iterator<String> names = root.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                if (!FIELDS.contains(name)) {
                    throw context.weirdStringException(name, RetrieveKnowledgeRequest.class,
                            "unknown retrieval request property");
                }
            }
            return new RetrieveKnowledgeRequest(text(root, "query", parser), integer(root, "topK", parser),
                    decimal(root, "similarityThreshold", parser));
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

        private static Double decimal(JsonNode root, String field, JsonParser parser) throws IOException {
            JsonNode node = root.get(field);
            if (node == null || node.isNull()) {
                return null;
            }
            if (!node.isNumber()) {
                throw JsonMappingException.from(parser, field + " must be a JSON number");
            }
            return node.doubleValue();
        }
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
