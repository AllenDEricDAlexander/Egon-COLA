package top.egon.cola.component.rag.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import top.egon.cola.component.rag.exception.RagValidationException;
import top.egon.cola.component.rag.metadata.RagMetadataKeys;

import java.util.Map;

/**
 * Write intent for {@code RagIngestionService#ingest}.
 *
 * <p>Carries an already extracted document rather than a byte stream: the consumer normally persists
 * the text before embedding so that a retried embedding never re-reads or re-parses the original.
 *
 * <p>{@code attributes} are the caller's business metadata. They are merged over the structural
 * attributes of the document, with the caller winning on a shared key, and may never contain a
 * reserved identity key.
 */
public record RagIngestionCommand(@NotBlank
                                  @Pattern(regexp = "[A-Za-z0-9._:-]{1,128}")
                                  String collectionId,
                                  @NotBlank
                                  @Pattern(regexp = "[A-Za-z0-9._-]{1,128}")
                                  String documentId,
                                  @NotBlank String logicalModelName,
                                  @NotNull @Valid RagChunkingConfigDTO chunkingConfig,
                                  @NotNull @Valid ExtractedDocumentBO document,
                                  @Size(max = 32) Map<String, String> attributes) {

    public RagIngestionCommand {
        collectionId = collectionId == null ? null : collectionId.trim();
        documentId = documentId == null ? null : documentId.trim();
        logicalModelName = logicalModelName == null ? null : logicalModelName.trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        RagMetadataKeys.rejectReservedKeys(attributes);
    }

    /**
     * Merges the document's structural attributes with the caller's business attributes.
     *
     * @throws RagValidationException when the merged map would contain a reserved key
     */
    public Map<String, String> mergedAttributes() {
        Map<String, String> merged = new java.util.LinkedHashMap<>();
        if (document != null) {
            merged.putAll(document.attributes());
        }
        merged.putAll(attributes);
        RagMetadataKeys.rejectReservedKeys(merged);
        return Map.copyOf(merged);
    }
}
