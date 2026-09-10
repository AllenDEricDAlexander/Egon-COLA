package top.egon.cola.component.rag.support;

import top.egon.cola.component.rag.exception.RagStorageException;
import top.egon.cola.component.rag.storage.RagDocumentStorage;
import top.egon.cola.component.rag.storage.RagDocumentStorageTypeEnum;
import top.egon.cola.component.rag.storage.RagStoredObjectBO;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** In-memory storage double with a failure hook, so no test touches the file system. */
public class FakeRagDocumentStorage implements RagDocumentStorage {

    /** Where the next call should fail. */
    public enum FailurePoint {
        NONE, STORE, OPEN, DELETE
    }

    private final Map<String, byte[]> objects = new LinkedHashMap<>();

    private FailurePoint failurePoint = FailurePoint.NONE;

    public void failOn(FailurePoint point) {
        this.failurePoint = point;
    }

    public int storedCount() {
        return objects.size();
    }

    @Override
    public RagDocumentStorageTypeEnum type() {
        return RagDocumentStorageTypeEnum.LOCAL;
    }

    @Override
    public RagStoredObjectBO store(String collectionId, String documentId, String fileName, InputStream content) {
        if (failurePoint == FailurePoint.STORE) {
            throw new RagStorageException("simulated store failure");
        }
        byte[] bytes = read(content);
        objects.put(key(collectionId, documentId), bytes);
        return new RagStoredObjectBO(collectionId, documentId, type(), bytes.length, Instant.EPOCH);
    }

    @Override
    public InputStream open(String collectionId, String documentId) {
        if (failurePoint == FailurePoint.OPEN) {
            throw new RagStorageException("simulated open failure");
        }
        byte[] bytes = objects.get(key(collectionId, documentId));
        if (bytes == null) {
            throw new RagStorageException("stored object not found");
        }
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public void delete(String collectionId, String documentId) {
        if (failurePoint == FailurePoint.DELETE) {
            throw new RagStorageException("simulated delete failure");
        }
        objects.remove(key(collectionId, documentId));
    }

    private static String key(String collectionId, String documentId) {
        return collectionId + "/" + documentId;
    }

    private static byte[] read(InputStream content) {
        try {
            return content.readAllBytes();
        } catch (IOException exception) {
            throw new RagStorageException("failed to read test content", exception);
        }
    }
}
