package top.egon.cola.component.rag.storage;

/**
 * Closed set of supported document storage backings.
 *
 * <p>Append-only: introducing an object-storage implementation adds a value here without changing
 * the meaning of {@link #LOCAL}.
 */
public enum RagDocumentStorageTypeEnum {

    /** Local file system. Not shareable across instances of a multi-instance deployment. */
    LOCAL
}
