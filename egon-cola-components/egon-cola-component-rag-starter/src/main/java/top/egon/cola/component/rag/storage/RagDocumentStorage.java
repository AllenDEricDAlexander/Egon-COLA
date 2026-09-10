package top.egon.cola.component.rag.storage;

import top.egon.cola.component.rag.exception.RagStorageException;
import top.egon.cola.component.rag.exception.RagValidationException;

import java.io.InputStream;

/**
 * Keeps the raw uploaded document next to the extracted text.
 *
 * <p>An extension point because the backing store is a deployment decision: the built-in local
 * implementation is enough for a single instance, and an object-storage implementation replaces it
 * without touching the engine.
 *
 * <p>Implementations must reject an identifier that could escape their namespace, must not close
 * the stream they are given, must treat a missing target as an error on read and as success on
 * delete.
 */
public interface RagDocumentStorage {

    RagDocumentStorageTypeEnum type();

    /** @throws RagValidationException for a malformed identifier or a null stream */
    RagStoredObjectBO store(String collectionId, String documentId, String fileName, InputStream content);

    /** @throws RagStorageException when the target does not exist or cannot be read */
    InputStream open(String collectionId, String documentId);

    /** Idempotent: deleting a target that is already gone succeeds. */
    void delete(String collectionId, String documentId);
}
