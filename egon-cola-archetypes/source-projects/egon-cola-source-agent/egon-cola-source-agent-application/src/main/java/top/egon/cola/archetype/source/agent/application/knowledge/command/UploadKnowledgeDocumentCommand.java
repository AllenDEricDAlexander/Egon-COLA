package top.egon.cola.archetype.source.agent.application.knowledge.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Normalized upload intent for one document of one knowledge base.
 *
 * <p>The bytes are carried in memory rather than as a stream: the use case stores the original and
 * extracts its text, and a stream can only be read once. The upload limit is enforced before either
 * read, so the buffer stays bounded by {@code agent.knowledge.runtime.max-upload-bytes}.
 *
 * <p>{@code displayName} defaults to the file name; the mime type and the file name are only hints
 * for the extractor routing, and the embedding configuration is never taken from a request.
 */
public record UploadKnowledgeDocumentCommand(
        @NotNull @Positive Long knowledgeBaseId,
        @NotBlank @Size(max = 255) String fileName,
        @Size(max = 128) String mimeType,
        @Size(max = 255) String displayName,
        @NotNull @Size(min = 1) byte[] content,
        @NotBlank @Size(max = 128) String traceId) {

    public UploadKnowledgeDocumentCommand {
        fileName = normalize(fileName);
        mimeType = normalize(mimeType);
        displayName = normalize(displayName) == null ? fileName : normalize(displayName);
        traceId = normalize(traceId);
    }

    /** The name a failure message and a retrieved chunk show for this document. */
    public String effectiveDisplayName() {
        return displayName == null ? fileName : displayName;
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
