package top.egon.cola.archetype.source.agent.domain.knowledge.model;

/**
 * Closed set of chunking strategies a knowledge base can be frozen with.
 *
 * <p>Append-only: the value names are persisted in {@code knowledge_base.chunk_strategy}, so
 * renaming an existing value would silently re-interpret stored rows.
 */
public enum ChunkingStrategyEnum {

    /** Token budget driven splitting. */
    TOKEN,

    /** Heading-aware splitting, degrading to token splitting for oversized sections. */
    MARKDOWN_HEADING,

    /** Separator-cascade splitting with a character-budget hard cut as the last resort. */
    RECURSIVE
}
