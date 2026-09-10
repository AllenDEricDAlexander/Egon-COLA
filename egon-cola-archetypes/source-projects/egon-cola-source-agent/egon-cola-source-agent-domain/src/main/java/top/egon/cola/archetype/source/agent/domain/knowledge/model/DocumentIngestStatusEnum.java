package top.egon.cola.archetype.source.agent.domain.knowledge.model;

import java.util.Set;

/**
 * Lifecycle of one uploaded document while it is being ingested.
 *
 * <p>Values are persisted in {@code knowledge_document.status} and mirror the outbox delivery
 * states; the legal moves are fixed by the design so a stale delivery can never rewind a finished
 * document.
 */
public enum DocumentIngestStatusEnum {

    /** Stored and queued, not yet picked up by a delivery attempt. */
    PENDING,

    /** One delivery attempt currently owns the document. */
    PROCESSING,

    /** Chunks are written; the document is searchable. */
    SUCCEEDED,

    /** The last attempt reported a recoverable failure. */
    FAILED,

    /** Delivery attempts are exhausted; only an explicit reprocess moves it again. */
    DEAD;

    private static final Set<DocumentIngestStatusEnum> TERMINAL =
            Set.of(SUCCEEDED, FAILED, DEAD);

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
