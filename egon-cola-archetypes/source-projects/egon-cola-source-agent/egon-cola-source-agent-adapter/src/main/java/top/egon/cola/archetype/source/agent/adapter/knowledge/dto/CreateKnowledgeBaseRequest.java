package top.egon.cola.archetype.source.agent.adapter.knowledge.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.ChunkingStrategyEnum;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * External JSON command that creates one knowledge base (Spec B §9.2.1).
 *
 * <p>Unknown properties are refused rather than dropped: the indexing configuration freezes at
 * creation, so a client must be told when it sent something that will not be stored. The optional
 * chunking fields carry their documented defaults by the time the record exists, which is what lets
 * the converter fill the primitive command fields.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
@JsonDeserialize(using = CreateKnowledgeBaseRequest.Deserializer.class)
@Schema(name = "CreateKnowledgeBaseRequest")
public record CreateKnowledgeBaseRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{2,64}") @Schema(example = "product-docs") String code,
        @NotBlank @Size(max = 128) @Schema(example = "产品文档库") String name,
        @Size(max = 512) @Schema(example = "内部产品手册") String description,
        @NotBlank @Size(max = 32) @Schema(example = "openai-small") String embeddingModel,
        @NotNull @Schema(allowableValues = {"TOKEN", "MARKDOWN_HEADING", "RECURSIVE"}, example = "TOKEN")
        ChunkingStrategyEnum chunkStrategy,
        @NotNull @Valid ChunkingConfigRequest chunkConfig) {

    public CreateKnowledgeBaseRequest {
        code = normalize(code);
        name = normalize(name);
        description = normalize(description);
        embeddingModel = normalize(embeddingModel);
    }

    /**
     * Request-side chunking parameters.
     *
     * <p>Only the field-level ranges live here; whether a field set matches the chosen strategy is
     * the domain's decision, so that one place owns the rule for every caller (§9.2.1).
     */
    public record ChunkingConfigRequest(
            @NotNull @Min(32) @Max(4096) @Schema(example = "512", minimum = "32", maximum = "4096")
            Integer maxTokensPerChunk,
            @Min(0) @Schema(example = "64", minimum = "0") Integer overlapTokens,
            @Min(0) @Max(4096) @Schema(example = "1", minimum = "0", maximum = "4096")
            Integer minChunkChars,
            @Schema(example = "[1,2,3]") List<@Min(1) @Max(6) Integer> headingLevels) {

        public ChunkingConfigRequest {
            overlapTokens = overlapTokens == null ? 0 : overlapTokens;
            minChunkChars = minChunkChars == null ? 1 : minChunkChars;
            headingLevels = headingLevels == null ? List.of() : List.copyOf(headingLevels);
        }
    }

    /**
     * Jackson-only boundary parser that refuses unknown fields instead of dropping them, and reads
     * each declared field with the type it declares.
     *
     * <p>A class-level {@code @JsonIgnoreProperties(ignoreUnknown = false)} cannot do this: whether
     * an unrecognized property throws is the mapper's {@code FAIL_ON_UNKNOWN_PROPERTIES} feature, and
     * the application mapper leaves it off. The published contract still refuses those fields, so the
     * refusal belongs to the type.
     */
    public static final class Deserializer extends StdDeserializer<CreateKnowledgeBaseRequest> {

        private static final Set<String> FIELDS =
                Set.of("code", "name", "description", "embeddingModel", "chunkStrategy", "chunkConfig");

        private static final Set<String> CONFIG_FIELDS =
                Set.of("maxTokensPerChunk", "overlapTokens", "minChunkChars", "headingLevels");

        public Deserializer() {
            super(CreateKnowledgeBaseRequest.class);
        }

        @Override
        public CreateKnowledgeBaseRequest deserialize(JsonParser parser, DeserializationContext context)
                throws IOException {
            return read(parser, context, "knowledge base request");
        }

        /** Reads the six published fields, which the create and the edit bodies both carry. */
        static CreateKnowledgeBaseRequest read(JsonParser parser, DeserializationContext context, String subject)
                throws IOException {
            JsonNode root = parser.getCodec().readTree(parser);
            requireObject(parser, root, subject);
            rejectUnknown(context, root, FIELDS, "unknown knowledge base request property");
            return new CreateKnowledgeBaseRequest(
                    text(root, "code", parser),
                    text(root, "name", parser),
                    text(root, "description", parser),
                    text(root, "embeddingModel", parser),
                    strategy(root.get("chunkStrategy"), parser),
                    chunkingConfig(root.get("chunkConfig"), parser, context));
        }

        /** Reads the frozen-configuration body shared by the create and the edit requests. */
        static ChunkingConfigRequest chunkingConfig(JsonNode node, JsonParser parser, DeserializationContext context)
                throws IOException {
            if (node == null || node.isNull()) {
                return null;
            }
            if (!node.isObject()) {
                throw JsonMappingException.from(parser, "chunkConfig must be a JSON object");
            }
            rejectUnknown(context, node, CONFIG_FIELDS, "unknown chunkConfig property");
            return new ChunkingConfigRequest(
                    integer(node, "maxTokensPerChunk", parser),
                    integer(node, "overlapTokens", parser),
                    integer(node, "minChunkChars", parser),
                    headingLevels(node.get("headingLevels"), parser));
        }

        static void requireObject(JsonParser parser, JsonNode root, String subject) throws IOException {
            if (root == null || !root.isObject()) {
                throw JsonMappingException.from(parser, subject + " must be a JSON object");
            }
        }

        static void rejectUnknown(DeserializationContext context, JsonNode node, Set<String> fields, String message)
                throws IOException {
            Iterator<String> names = node.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                if (!fields.contains(name)) {
                    throw context.weirdStringException(name, CreateKnowledgeBaseRequest.class, message);
                }
            }
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

        private static ChunkingStrategyEnum strategy(JsonNode node, JsonParser parser) throws IOException {
            if (node == null || node.isNull()) {
                return null;
            }
            if (!node.isTextual()) {
                throw JsonMappingException.from(parser, "chunkStrategy must be a JSON string");
            }
            try {
                return ChunkingStrategyEnum.valueOf(node.textValue());
            } catch (IllegalArgumentException unsupported) {
                throw JsonMappingException.from(parser, "unsupported chunkStrategy");
            }
        }

        private static List<Integer> headingLevels(JsonNode node, JsonParser parser) throws IOException {
            if (node == null || node.isNull()) {
                return null;
            }
            if (!node.isArray()) {
                throw JsonMappingException.from(parser, "headingLevels must be a JSON array");
            }
            List<Integer> levels = new ArrayList<>(node.size());
            for (JsonNode level : node) {
                if (!level.isIntegralNumber() || !level.canConvertToInt()) {
                    throw JsonMappingException.from(parser, "headingLevels must only contain integers");
                }
                levels.add(level.intValue());
            }
            return levels;
        }
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
