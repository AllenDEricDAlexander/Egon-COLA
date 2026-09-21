package top.egon.cola.archetype.source.agent.domain.knowledge.model;

import top.egon.cola.component.common.core.enums.EgonEnum;

import java.util.Set;

/**
 * Lifecycle of one uploaded document while it is being ingested.
 *
 * <p>Values are persisted in {@code knowledge_document.status} and mirror the outbox delivery
 * states; the legal moves are fixed by the design so a stale delivery can never rewind a finished
 * document. Codes are declared rather than derived from {@link #ordinal()} so a reordering cannot
 * renumber a stored status.
 */
public enum DocumentIngestStatusEnum implements EgonEnum {

    /** Stored and queued, not yet picked up by a delivery attempt. */
    PENDING(0, "document is queued for ingestion"),

    /** One delivery attempt currently owns the document. */
    PROCESSING(1, "document ingestion is running"),

    /** Chunks are written; the document is searchable. */
    SUCCEEDED(2, "document ingestion succeeded"),

    /** The last attempt reported a recoverable failure. */
    FAILED(3, "document ingestion failed"),

    /** Delivery attempts are exhausted; only an explicit reprocess moves it again. */
    DEAD(4, "document ingestion is exhausted");

    private static final Set<DocumentIngestStatusEnum> TERMINAL =
            Set.of(SUCCEEDED, FAILED, DEAD);

    private final int code;

    private final String message;

    DocumentIngestStatusEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    /** A terminal document is no longer advanced by delivery attempts, only by reprocessing. */
    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    /**
     * @return whether a delivery attempt, a failure or a reprocess may move this status to
     *         {@code next}; a status never transitions to itself
     */
    public boolean canTransitionTo(DocumentIngestStatusEnum next) {
        if (next == null || next == this) {
            return false;
        }
        return switch (this) {
            case PENDING -> next == PROCESSING;
            case PROCESSING -> next == SUCCEEDED || next == PENDING || next == DEAD;
            case SUCCEEDED, FAILED, DEAD -> next == PENDING;
        };
    }
}
