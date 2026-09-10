package top.egon.cola.archetype.source.agent.domain.knowledge.model;

import java.util.List;

/**
 * Chunking parameters of one knowledge base, without the strategy they belong to.
 *
 * <p>Frozen once documents exist: changing them would make stored chunks unreproducible, so a
 * knowledge base must be recreated instead. The parameters are persisted together in
 * {@code knowledge_base.chunk_config}; the strategy travels beside them in
 * {@link KnowledgeBaseBO#chunkStrategy()}, which is what makes the per-strategy field rules
 * checkable.
 */
public record KnowledgeChunkConfigBO(int maxTokensPerChunk,
                                     int overlapTokens,
                                     int minChunkChars,
                                     List<Integer> headingLevels) {

    public static final int MIN_MAX_TOKENS = 32;

    public static final int MAX_MAX_TOKENS = 4096;

    public KnowledgeChunkConfigBO {
        if (maxTokensPerChunk < MIN_MAX_TOKENS || maxTokensPerChunk > MAX_MAX_TOKENS) {
            throw new IllegalArgumentException(
                    "maxTokensPerChunk must be between " + MIN_MAX_TOKENS + " and " + MAX_MAX_TOKENS);
        }
        overlapTokens = Math.max(overlapTokens, 0);
        if (overlapTokens >= maxTokensPerChunk) {
            throw new IllegalArgumentException("overlapTokens must be smaller than maxTokensPerChunk");
        }
        minChunkChars = minChunkChars <= 0 ? 1 : minChunkChars;
        if (minChunkChars > MAX_MAX_TOKENS) {
            throw new IllegalArgumentException("minChunkChars must not exceed " + MAX_MAX_TOKENS);
        }
        headingLevels = normalizeHeadingLevels(headingLevels);
    }

    /** Whether this configuration carries heading levels, which only a heading strategy allows. */
    public boolean hasHeadingLevels() {
        return !headingLevels.isEmpty();
    }

    private static List<Integer> normalizeHeadingLevels(List<Integer> levels) {
        if (levels == null || levels.isEmpty()) {
            return List.of();
        }
        if (levels.stream().anyMatch(level -> level == null || level < 1 || level > 6)) {
            throw new IllegalArgumentException("headingLevels values must be between 1 and 6");
        }
        return levels.stream().distinct().sorted().toList();
    }
}
