package top.egon.cola.component.rag.chunk;

import top.egon.cola.component.rag.exception.RagValidationException;

/**
 * Derives the stable identity of a chunk from its document and position.
 *
 * <p>This derivation is what makes ingestion safe to re-run: because the vector store offers no way
 * to enumerate stored documents, the component can only rebuild a document's chunks, and rebuilding
 * is equivalent to re-writing them only when the identifiers are deterministic.
 */
public class RagChunkIdFactory {

    private static final char SEPARATOR = ':';

    /** @throws RagValidationException for a blank or malformed document id, or a negative index */
    public String create(String documentId, int chunkIndex) {
        if (documentId == null || documentId.isBlank()) {
            throw new RagValidationException("documentId must not be blank");
        }
        if (documentId.indexOf(SEPARATOR) >= 0) {
            throw new RagValidationException("documentId must not contain ':'");
        }
        if (chunkIndex < 0) {
            throw new RagValidationException("chunkIndex must not be negative");
        }
        return documentId + SEPARATOR + chunkIndex;
    }
}
