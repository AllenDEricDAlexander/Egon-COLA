package top.egon.cola.archetype.source.agent.domain.knowledge.model;

import java.time.Instant;
import java.util.regex.Pattern;

/**
 * One knowledge base: its metadata plus the indexing configuration frozen at creation time.
 *
 * <p>Carries no audit or soft-delete columns; those belong to the persistence object. The
 * identifier is {@code null} until the base has been stored, and the timestamps are the server
 * stamps of the stored row.
 */
public record KnowledgeBaseBO(
        Long knowledgeBaseId,
        Long tenantId,
        String code,
        String name,
        String description,
        String embeddingModel,
        ChunkingStrategyEnum chunkStrategy,
        KnowledgeChunkConfigBO chunkConfig,
        KnowledgeBaseStatusEnum status,
        Instant createdAt,
        Instant updatedAt) {

    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Za-z0-9._-]{2,64}");

    private static final int MAX_NAME_LENGTH = 128;

    private static final int MAX_DESCRIPTION_LENGTH = 512;

    private static final int MAX_EMBEDDING_MODEL_LENGTH = 32;

    public KnowledgeBaseBO {
        if (knowledgeBaseId != null && knowledgeBaseId <= 0) {
            throw new IllegalArgumentException("knowledgeBaseId must be positive");
        }
        tenantId = requireTenantId(tenantId);
        code = requireCode(code);
        name = requireName(name);
        description = normalizeDescription(description);
        embeddingModel = requireEmbeddingModel(embeddingModel);
        if (chunkStrategy == null) {
            throw new IllegalArgumentException("chunkStrategy must not be null");
        }
        chunkConfig = requireChunkConfig(chunkConfig, chunkStrategy);
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
    }

    /** Creates a base that has not been stored yet: no identifier, active, unstamped. */
    public static KnowledgeBaseBO create(String code, String name, String description, String embeddingModel,
                                         ChunkingStrategyEnum chunkStrategy, KnowledgeChunkConfigBO chunkConfig,
                                         Long tenantId) {
        return new KnowledgeBaseBO(null, tenantId, code, name, description, embeddingModel,
                chunkStrategy, chunkConfig, KnowledgeBaseStatusEnum.ACTIVE, null, null);
    }

    private static Long requireTenantId(Long value) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException("tenantId must not be negative");
        }
        return value;
    }

    private static String requireCode(String value) {
        String normalized = requireText(value, "code");
        if (!CODE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("code must be 2 to 64 characters of [A-Za-z0-9._-]");
        }
        return normalized;
    }

    private static String requireName(String value) {
        String normalized = requireText(value, "name");
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("name must not exceed " + MAX_NAME_LENGTH + " characters");
        }
        return normalized;
    }

    private static String normalizeDescription(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                    "description must not exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        return normalized;
    }

    private static String requireEmbeddingModel(String value) {
        String normalized = requireText(value, "embeddingModel");
        if (normalized.length() > MAX_EMBEDDING_MODEL_LENGTH) {
            throw new IllegalArgumentException(
                    "embeddingModel must not exceed " + MAX_EMBEDDING_MODEL_LENGTH + " characters");
        }
        return normalized;
    }

    private static KnowledgeChunkConfigBO requireChunkConfig(KnowledgeChunkConfigBO value,
                                                            ChunkingStrategyEnum strategy) {
        if (value == null) {
            throw new IllegalArgumentException("chunkConfig must not be null");
        }
        if (value.hasHeadingLevels() && strategy != ChunkingStrategyEnum.MARKDOWN_HEADING) {
            throw new IllegalArgumentException("headingLevels are only valid for MARKDOWN_HEADING");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
