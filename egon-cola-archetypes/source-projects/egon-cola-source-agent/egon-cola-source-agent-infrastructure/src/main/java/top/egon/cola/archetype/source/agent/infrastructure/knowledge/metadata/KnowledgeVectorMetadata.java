package top.egon.cola.archetype.source.agent.infrastructure.knowledge.metadata;

/**
 * Metadata keys the knowledge domain adds to every chunk it writes.
 *
 * <p>They sit next to the keys the RAG component reserves for itself; the component rejects a caller
 * that tries to supply one of its own, and this one is likewise never left to a caller: ingestion
 * writes it and retrieval overwrites whatever it was given with the tenant of the running thread.
 */
public final class KnowledgeVectorMetadata {

    /** The tenant every chunk belongs to and every retrieval filters on. */
    public static final String TENANT_ID = "tenantId";

    private KnowledgeVectorMetadata() {
    }
}
