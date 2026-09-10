package top.egon.cola.component.rag.model;

import jakarta.validation.constraints.NotNull;

import java.io.InputStream;

/**
 * Write intent for {@code RagExtractionService#extract}.
 *
 * <p>Both hints may be {@code null}; routing then depends entirely on how each registered extractor
 * treats a missing mime type and file name. {@code content} is read once and never closed by the
 * component.
 */
public record RagExtractionCommand(String fileName, String mimeType, @NotNull InputStream content) {

    public RagExtractionCommand {
        fileName = fileName == null ? null : fileName.trim();
        mimeType = mimeType == null || !mimeType.contains("/") ? null : mimeType.trim();
    }
}
