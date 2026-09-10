package top.egon.cola.archetype.source.agent.application.knowledge.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.ChunkingStrategyEnum;

import java.util.List;

/**
 * Normalized edit intent for one knowledge base: the name and the description, and nothing else.
 *
 * <p>The frozen indexing fields are still carried, as nullable probes rather than as accepted input.
 * The request boundary binds them instead of silently dropping them, so a caller that sends an
 * immutable field gets a rejection naming that field instead of believing the value took effect
 * (Spec B §9.2.4 step 3). A {@code null} probe means the caller did not send the field.
 */
public record UpdateKnowledgeBaseCommand(
        @NotNull @Positive Long knowledgeBaseId,
        @NotBlank @Size(max = 128) String name,
        @Size(max = 512) String description,
        String code,
        String embeddingModel,
        ChunkingStrategyEnum chunkStrategy,
        @Valid CreateKnowledgeBaseCommand.ChunkingConfigCommand chunkConfig,
        @NotBlank @Size(max = 128) String traceId) {

    public UpdateKnowledgeBaseCommand {
        name = normalize(name);
        description = normalize(description);
        code = normalize(code);
        embeddingModel = normalize(embeddingModel);
        traceId = normalize(traceId);
    }

    /**
     * @return the create-only fields the caller sent, in the order they are reported; empty for a
     *         request that only edits the editable metadata
     */
    public List<String> immutableFields() {
        List<String> sent = new java.util.ArrayList<>(4);
        if (code != null) {
            sent.add("code");
        }
        if (embeddingModel != null) {
            sent.add("embeddingModel");
        }
        if (chunkStrategy != null) {
            sent.add("chunkStrategy");
        }
        if (chunkConfig != null) {
            sent.add("chunkConfig");
        }
        return List.copyOf(sent);
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
