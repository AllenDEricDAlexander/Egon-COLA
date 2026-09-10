package top.egon.cola.archetype.source.agent.adapter.knowledge.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.ChunkingStrategyEnum;

import java.io.IOException;

/**
 * External JSON command that edits the mutable metadata of one knowledge base (Spec B §9.2.4).
 *
 * <p>{@code name} and {@code description} are the complete writable representation; a missing
 * description and an explicit {@code null} both clear it, because {@code PUT} replaces the whole
 * representation. The create-only fields are bound as probes instead of being dropped by the binder,
 * so the use case can name the offending field rather than let the caller believe the edit took
 * effect (Spec B §9.2.4 step 3). {@code null} means the caller did not send the field.
 *
 * <p>Anything else is unknown to this contract and refused: silently ignoring it would hide a
 * typo'd field name behind a successful response.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
@JsonDeserialize(using = UpdateKnowledgeBaseRequest.Deserializer.class)
@Schema(name = "UpdateKnowledgeBaseRequest")
public record UpdateKnowledgeBaseRequest(
        @NotBlank @Size(max = 128) @Schema(example = "产品文档库（内网）") String name,
        @Size(max = 512) @Schema(example = "内网产品手册") String description,
        String code,
        String embeddingModel,
        ChunkingStrategyEnum chunkStrategy,
        @Valid CreateKnowledgeBaseRequest.ChunkingConfigRequest chunkConfig) {

    public UpdateKnowledgeBaseRequest {
        name = normalize(name);
        description = normalize(description);
        code = normalize(code);
        embeddingModel = normalize(embeddingModel);
    }

    /**
     * Jackson-only boundary parser for the edit body.
     *
     * <p>It reads the same six fields as the create body — the four create-only ones are the probes
     * this use case must see — so the parsing lives with the create request and this class only takes
     * the result apart. The refusal of unknown fields therefore cannot drift between the two bodies.
     */
    public static final class Deserializer extends StdDeserializer<UpdateKnowledgeBaseRequest> {

        public Deserializer() {
            super(UpdateKnowledgeBaseRequest.class);
        }

        @Override
        public UpdateKnowledgeBaseRequest deserialize(JsonParser parser, DeserializationContext context)
                throws IOException {
            CreateKnowledgeBaseRequest body = CreateKnowledgeBaseRequest.Deserializer.read(
                    parser, context, "knowledge base edit request");
            return new UpdateKnowledgeBaseRequest(body.name(), body.description(), body.code(),
                    body.embeddingModel(), body.chunkStrategy(), body.chunkConfig());
        }
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
