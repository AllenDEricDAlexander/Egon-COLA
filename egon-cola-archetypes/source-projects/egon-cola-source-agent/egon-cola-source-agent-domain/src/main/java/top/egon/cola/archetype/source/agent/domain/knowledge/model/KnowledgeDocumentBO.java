package top.egon.cola.archetype.source.agent.domain.knowledge.model;

import java.time.Instant;
import java.util.regex.Pattern;

/**
 * One uploaded document: the stored original, the extracted text and the ingest status.
 *
 * <p>The text is {@code null} only for a row that has been soft deleted by the collection-wide
 * delete path; an uploaded document always carries it. Failure details are cleared whenever the
 * document re-enters {@link DocumentIngestStatusEnum#PENDING}.
 */
public record KnowledgeDocumentBO(
        Long documentId,
        Long tenantId,
        Long knowledgeBaseId,
        String displayName,
        String fileName,
        String mimeType,
        long sizeBytes,
        String contentHash,
        String storageType,
        String storageKey,
        String content,
        DocumentIngestStatusEnum status,
        int chunkCount,
        int attemptCount,
        String errorCode,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt) {

    private static final Pattern CONTENT_HASH_PATTERN = Pattern.compile("[0-9a-f]{64}");

    private static final int MAX_DISPLAY_NAME_LENGTH = 255;

    private static final int MAX_FILE_NAME_LENGTH = 255;

    private static final int MAX_MIME_TYPE_LENGTH = 128;

    private static final int MAX_STORAGE_TYPE_LENGTH = 16;

    private static final int MAX_STORAGE_KEY_LENGTH = 512;

    private static final int MAX_ERROR_CODE_LENGTH = 64;

    private static final int MAX_ERROR_MESSAGE_LENGTH = 512;

    public KnowledgeDocumentBO {
        if (documentId != null && documentId <= 0) {
            throw new IllegalArgumentException("documentId must be positive");
        }
        tenantId = requireTenantId(tenantId);
        if (knowledgeBaseId == null || knowledgeBaseId <= 0) {
            throw new IllegalArgumentException("knowledgeBaseId must be positive");
        }
        displayName = requireText(displayName, "displayName", MAX_DISPLAY_NAME_LENGTH);
        fileName = normalizeText(fileName, MAX_FILE_NAME_LENGTH);
        mimeType = normalizeText(mimeType, MAX_MIME_TYPE_LENGTH);
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must not be negative");
        }
        contentHash = requireContentHash(contentHash);
        storageType = requireText(storageType, "storageType", MAX_STORAGE_TYPE_LENGTH);
        storageKey = requireText(storageKey, "storageKey", MAX_STORAGE_KEY_LENGTH);
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (chunkCount < 0) {
            throw new IllegalArgumentException("chunkCount must not be negative");
        }
        if (attemptCount < 0) {
            throw new IllegalArgumentException("attemptCount must not be negative");
        }
        errorCode = normalizeText(errorCode, MAX_ERROR_CODE_LENGTH);
        errorMessage = normalizeText(errorMessage, MAX_ERROR_MESSAGE_LENGTH);
    }

    /** Creates a document that is stored and queued, before any delivery attempt has run. */
    public static KnowledgeDocumentBO create(Long tenantId, Long knowledgeBaseId, String displayName,
                                             String fileName, String mimeType, long sizeBytes,
                                             String contentHash, String storageType, String storageKey,
                                             String content) {
        return new KnowledgeDocumentBO(null, tenantId, knowledgeBaseId, displayName, fileName, mimeType,
                sizeBytes, contentHash, storageType, storageKey, content,
                DocumentIngestStatusEnum.PENDING, 0, 0, null, null, null, null);
    }

    private static Long requireTenantId(Long value) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException("tenantId must not be negative");
        }
        return value;
    }

    private static String requireContentHash(String value) {
        String normalized = requireText(value, "contentHash", 64);
        if (!CONTENT_HASH_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("contentHash must be a lower case SHA-256 hex digest");
        }
        return normalized;
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalizeText(value, maxLength);
    }

    private static String normalizeText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException("value must not exceed " + maxLength + " characters");
        }
        return normalized;
    }
}
