package top.egon.cola.component.rag.autoconfigure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import top.egon.cola.component.rag.storage.RagDocumentStorageTypeEnum;

/** Document storage selection and the local backing directory. */
public record RagStorageProperties(@NotNull RagDocumentStorageTypeEnum type,
                                   @NotNull @Valid RagLocalStorageProperties local) {

    public static final String DEFAULT_LOCAL_ROOT = "./data/rag-documents";

    public RagStorageProperties {
        type = type == null ? RagDocumentStorageTypeEnum.LOCAL : type;
        local = local == null ? new RagLocalStorageProperties(null) : local;
    }

    /** Local file system backing; the root is created on first write. */
    public record RagLocalStorageProperties(String root) {

        public RagLocalStorageProperties {
            root = (root == null || root.isBlank()) ? DEFAULT_LOCAL_ROOT : root.trim();
        }
    }
}
