package top.egon.cola.archetype.source.agent.common.knowledge;

/**
 * The queue channel an upload is announced on, and the fields of its payload.
 *
 * <p>Shared on purpose: the producer is an application use case and the handler is infrastructure,
 * which cannot see each other. A literal repeated on both sides would fail silently — an unknown
 * channel is refused at enqueue time, but a renamed payload field is only discovered by a failed
 * delivery, long after the upload answered {@code 202}.
 */
public final class KnowledgeIngestChannel {

    /** Channel name, registered by the ingest handler as the one it owns. */
    public static final String NAME = "rag-ingest";

    /** Envelope version both sides understand; a change is a version bump, never an edit. */
    public static final String SCHEMA_VERSION = "1";

    public static final String SCHEMA_VERSION_FIELD = "schemaVersion";

    public static final String DOCUMENT_ID_FIELD = "documentId";

    /** The tenant the delivery thread has to restore before it reads anything. */
    public static final String TENANT_ID_FIELD = "tenantId";

    private KnowledgeIngestChannel() {
    }
}
