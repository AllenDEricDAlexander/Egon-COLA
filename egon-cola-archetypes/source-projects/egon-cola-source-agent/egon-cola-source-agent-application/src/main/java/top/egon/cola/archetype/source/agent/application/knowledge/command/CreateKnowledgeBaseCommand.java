package top.egon.cola.archetype.source.agent.application.knowledge.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.ChunkingStrategyEnum;

import java.util.List;

/**
 * Normalized create intent for one knowledge base.
 *
 * <p>Carries no tenant: the tenant is the request-scoped MDC value the persistence layer applies
 * (Spec B §9.2.1), never a field a caller could set. The indexing configuration is supplied exactly
 * once, because it freezes with the base and no later use case accepts it.
 */
public record CreateKnowledgeBaseCommand(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{2,64}") String code,
        @NotBlank @Size(max = 128) String name,
        @Size(max = 512) String description,
        @NotBlank @Size(max = 32) String embeddingModel,
        @NotNull ChunkingStrategyEnum chunkStrategy,
        @NotNull @Valid ChunkingConfigCommand chunkConfig,
        @NotBlank @Size(max = 128) String traceId) {

    public CreateKnowledgeBaseCommand {
        code = normalize(code);
        name = normalize(name);
        description = normalize(description);
        embeddingModel = normalize(embeddingModel);
        traceId = normalize(traceId);
    }

    /**
     * The chunking parameters as they arrive, before the domain bounds are applied.
     *
     * <p>{@code minChunkChars} accepts the published range including zero: the domain reads a
     * non-positive value as "no minimum beyond one character", so rejecting zero here would refuse a
     * request the contract defines.
     */
    public record ChunkingConfigCommand(
            @Min(32) @Max(4096) int maxTokensPerChunk,
            @Min(0) int overlapTokens,
            @Min(0) @Max(4096) int minChunkChars,
            List<@Min(1) @Max(6) Integer> headingLevels) {

        public ChunkingConfigCommand {
            headingLevels = headingLevels == null ? List.of() : List.copyOf(headingLevels);
        }
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
