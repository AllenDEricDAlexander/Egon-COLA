package top.egon.cola.component.rag.chunk;

/**
 * Closed set of chunking strategies, replacing string dispatch.
 *
 * <p>Append-only: changing the meaning of an existing value would break the reproducibility that
 * ingestion re-runs depend on.
 */
public enum RagChunkingStrategyEnum {

    /** Token budget driven splitting, layered on the framework's token splitter. */
    TOKEN,

    /** Heading-aware splitting that degrades to token splitting for oversized sections. */
    MARKDOWN_HEADING,

    /** Separator-cascade splitting with a character-budget hard cut as the last resort. */
    RECURSIVE
}
