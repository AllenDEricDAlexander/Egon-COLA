package top.egon.cola.component.rag.model;

import top.egon.cola.component.rag.exception.RagValidationException;

import java.util.Map;

/**
 * One piece of a document, in document order.
 *
 * <p>Deliberately carries no identity: the chunk id is derived by the component from the document
 * id and the index, so a strategy cannot forge an identity that would break re-run idempotency.
 */
public record RagChunkBO(int chunkIndex, String content, Map<String, String> attributes) {

    public RagChunkBO {
        if (chunkIndex < 0) {
            throw new RagValidationException("chunk index must not be negative");
        }
        if (content == null) {
            throw new RagValidationException("chunk content must not be null");
        }
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
