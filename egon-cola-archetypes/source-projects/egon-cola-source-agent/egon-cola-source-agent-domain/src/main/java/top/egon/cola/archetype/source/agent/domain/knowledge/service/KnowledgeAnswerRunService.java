package top.egon.cola.archetype.source.agent.domain.knowledge.service;

/**
 * Cancellable handle for one in-flight answer generation.
 *
 * <p>Cancelling is idempotent and ends the stream: the model subscription is released, no further
 * event is published, and the capacity permit the use case holds is released by the use case itself
 * when its own terminal bookkeeping runs.
 */
public interface KnowledgeAnswerRunService {

    void cancel();
}
