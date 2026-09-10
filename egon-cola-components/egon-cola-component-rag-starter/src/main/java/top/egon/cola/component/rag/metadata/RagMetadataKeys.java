package top.egon.cola.component.rag.metadata;

import top.egon.cola.component.rag.exception.RagValidationException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Reserved vector metadata keys and the rules that keep them component-owned.
 *
 * <p>Callers may attach arbitrary business attributes, but never these keys: they carry the identity
 * the component filters on, so letting a caller write them would let it forge cross-collection or
 * cross-model visibility. Offending keys are rejected rather than silently stripped.
 */
public final class RagMetadataKeys {

    public static final String COLLECTION_ID = "collectionId";

    public static final String DOCUMENT_ID = "documentId";

    public static final String CHUNK_INDEX = "chunkIndex";

    public static final String LOGICAL_MODEL_NAME = "logicalModelName";

    public static final String CONTENT_HASH = "contentHash";

    private static final Set<String> RESERVED =
            Set.of(COLLECTION_ID, DOCUMENT_ID, CHUNK_INDEX, LOGICAL_MODEL_NAME, CONTENT_HASH);

    private RagMetadataKeys() {
    }

    public static Set<String> reservedKeys() {
        return RESERVED;
    }

    /** @throws RagValidationException when any reserved key is present */
    public static void rejectReservedKeys(Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return;
        }
        Set<String> offending = new TreeSet<>(attributes.keySet());
        offending.retainAll(RESERVED);
        if (!offending.isEmpty()) {
            throw new RagValidationException("reserved metadata keys must not be supplied: " + offending);
        }
    }

    /**
     * Builds the complete metadata map for one chunk: reserved identity first, then the merged
     * attributes, which are re-checked so a later change cannot bypass the guard.
     */
    public static Map<String, Object> toDocumentMetadata(String collectionId, String documentId,
                                                         String logicalModelName, int chunkIndex,
                                                         Map<String, String> mergedAttributes) {
        rejectReservedKeys(mergedAttributes);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(COLLECTION_ID, collectionId);
        metadata.put(DOCUMENT_ID, documentId);
        metadata.put(LOGICAL_MODEL_NAME, logicalModelName);
        metadata.put(CHUNK_INDEX, chunkIndex);
        if (mergedAttributes != null) {
            metadata.putAll(mergedAttributes);
        }
        return Map.copyOf(metadata);
    }
}
