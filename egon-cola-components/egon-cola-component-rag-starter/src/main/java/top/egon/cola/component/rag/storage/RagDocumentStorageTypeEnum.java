package top.egon.cola.component.rag.storage;

import top.egon.cola.component.common.core.enums.EgonEnum;

/**
 * Closed set of supported document storage backings.
 *
 * <p>Append-only: introducing an object-storage implementation adds a value here without changing
 * the meaning of {@link #LOCAL}.
 */
public enum RagDocumentStorageTypeEnum implements EgonEnum {

    /** Local file system. Not shareable across instances of a multi-instance deployment. */
    LOCAL(0, "LOCAL");

    private final int code;

    private final String message;

    RagDocumentStorageTypeEnum(int code, String message) {
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
