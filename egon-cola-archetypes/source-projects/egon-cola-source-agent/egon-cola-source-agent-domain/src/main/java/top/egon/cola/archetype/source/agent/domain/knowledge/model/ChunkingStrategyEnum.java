package top.egon.cola.archetype.source.agent.domain.knowledge.model;

import top.egon.cola.component.common.core.enums.EgonEnum;

/**
 * Closed set of chunking strategies a knowledge base can be frozen with.
 *
 * <p>Append-only: the value names are persisted in {@code knowledge_base.chunk_strategy}, so
 * renaming an existing value would silently re-interpret stored rows. Codes are declared instead of
 * derived from the declaration order for the same reason.
 */
public enum ChunkingStrategyEnum implements EgonEnum {

    /** Token budget driven splitting. */
    TOKEN(0, "token budget driven splitting"),

    /** Heading-aware splitting, degrading to token splitting for oversized sections. */
    MARKDOWN_HEADING(1, "heading aware splitting"),

    /** Separator-cascade splitting with a character-budget hard cut as the last resort. */
    RECURSIVE(2, "separator cascade splitting");

    private final int code;

    private final String message;

    ChunkingStrategyEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
