package top.egon.cola.archetype.source.agent.adapter.knowledge.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.DocumentIngestStatusEnum;

import java.time.Instant;

/**
 * Public representation of one uploaded document (Spec B §9.2.6, §9.2.8, §9.2.9).
 *
 * <p>The stored text is deliberately absent: {@code contentChars} states how much of it was persisted,
 * which is what the polling path needs to confirm the text reached the database without transporting
 * a large field on every poll. The storage location and the content fingerprint are absent for the
 * same reason — they are how this application finds the original, not what a caller is promised.
 *
 * <p>Absent fields are omitted rather than sent as {@code null}: a document that carries no failure
 * has no failure to render.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "KnowledgeDocumentVO")
public record KnowledgeDocumentVO(
        @Schema(example = "9") Long documentId,
        @Schema(example = "7") Long knowledgeBaseId,
        @Schema(example = "2026 手册") String displayName,
        @Schema(example = "report.pdf") String fileName,
        @Schema(example = "application/pdf") String mimeType,
        @Schema(example = "182734") Long sizeBytes,
        @Schema(allowableValues = {"PENDING", "PROCESSING", "SUCCEEDED", "FAILED", "DEAD"}, example = "PENDING")
        DocumentIngestStatusEnum status,
        @Schema(example = "42") int chunkCount,
        @Schema(example = "0", minimum = "0") Integer attemptCount,
        @Schema(description = "Characters of persisted text; the text itself is never returned",
                example = "128400", minimum = "0") Integer contentChars,
        @Schema(example = "KNOWLEDGE_EMBEDDING_FAILED") String errorCode,
        @Schema(example = "document ingestion failed") String errorMessage,
        @Schema(format = "date-time") Instant createdAt,
        @Schema(format = "date-time") Instant updatedAt,
        @Schema(example = "4e9d6938") String traceId) {
}
