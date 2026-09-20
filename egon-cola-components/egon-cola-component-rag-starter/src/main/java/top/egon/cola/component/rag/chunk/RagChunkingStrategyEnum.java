package top.egon.cola.component.rag.chunk;

import top.egon.cola.component.common.core.enums.EgonEnum;

/**
 * Closed set of chunking strategies, replacing string dispatch.
 *
 * <p>Append-only: changing the meaning of an existing value would break the reproducibility that
 * ingestion re-runs depend on.
 */
public enum RagChunkingStrategyEnum implements EgonEnum {

    /** Token budget driven splitting, layered on the framework's token splitter. */
    TOKEN(0, "TOKEN"),

    /** Heading-aware splitting that degrades to token splitting for oversized sections. */
    MARKDOWN_HEADING(1, "MARKDOWN_HEADING"),

    /** Separator-cascade splitting with a character-budget hard cut as the last resort. */
    RECURSIVE(2, "RECURSIVE");

    private final int code;

    private final String message;

    RagChunkingStrategyEnum(int code, String message) {
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
