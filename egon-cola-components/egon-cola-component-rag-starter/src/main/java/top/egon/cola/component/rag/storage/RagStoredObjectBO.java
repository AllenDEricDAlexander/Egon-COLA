package top.egon.cola.component.rag.storage;

import top.egon.cola.component.rag.exception.RagValidationException;

import java.time.Instant;

/** What a storage implementation reports after accepting a document. */
public record RagStoredObjectBO(String collectionId,
                                String documentId,
                                RagDocumentStorageTypeEnum storageType,
                                long sizeBytes,
                                Instant storedAt) {

    public RagStoredObjectBO {
        if (sizeBytes < 0) {
            throw new RagValidationException("sizeBytes must not be negative");
        }
        if (storedAt == null) {
            throw new RagValidationException("storedAt must not be null");
        }
    }
}
