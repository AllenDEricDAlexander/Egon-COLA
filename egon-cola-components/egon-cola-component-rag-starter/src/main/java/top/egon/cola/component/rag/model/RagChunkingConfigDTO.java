package top.egon.cola.component.rag.model;

import top.egon.cola.component.rag.chunk.RagChunkingStrategyEnum;
import top.egon.cola.component.rag.exception.RagValidationException;

import java.util.List;

/**
 * Chunking strategy and its parameters.
 *
 * <p>Frozen per collection: changing these values after data exists would make the stored chunks
 * unreproducible, so a caller must recreate the collection instead.
 */
public record RagChunkingConfigDTO(RagChunkingStrategyEnum strategy,
                                   int maxTokensPerChunk,
                                   int overlapTokens,
                                   int minChunkChars,
                                   List<Integer> headingLevels) {

    public static final int MIN_MAX_TOKENS = 32;

    public static final int MAX_MAX_TOKENS = 4096;

    public RagChunkingConfigDTO {
        if (strategy == null) {
            throw new RagValidationException("strategy must not be null");
        }
        overlapTokens = Math.max(overlapTokens, 0);
        minChunkChars = minChunkChars <= 0 ? 1 : minChunkChars;
        if (maxTokensPerChunk < MIN_MAX_TOKENS || maxTokensPerChunk > MAX_MAX_TOKENS) {
            throw new RagValidationException("maxTokensPerChunk must be between "
                    + MIN_MAX_TOKENS + " and " + MAX_MAX_TOKENS);
        }
        if (overlapTokens >= maxTokensPerChunk) {
            throw new RagValidationException("overlapTokens must be smaller than maxTokensPerChunk");
        }
        if (minChunkChars > MAX_MAX_TOKENS) {
            throw new RagValidationException("minChunkChars must not exceed " + MAX_MAX_TOKENS);
        }
        headingLevels = normalizeHeadingLevels(strategy, headingLevels);
    }

    private static List<Integer> normalizeHeadingLevels(RagChunkingStrategyEnum strategy, List<Integer> levels) {
        boolean provided = levels != null && !levels.isEmpty();
        if (strategy != RagChunkingStrategyEnum.MARKDOWN_HEADING) {
            if (provided) {
                throw new RagValidationException("headingLevels is only valid for MARKDOWN_HEADING");
            }
            return List.of();
        }
        if (!provided) {
            return List.of(1, 2, 3);
        }
        if (levels.stream().anyMatch(level -> level == null || level < 1 || level > 6)) {
            throw new RagValidationException("headingLevels values must be between 1 and 6");
        }
        return levels.stream().distinct().sorted().toList();
    }
}
