package top.egon.cola.archetype.source.agent.domain.knowledge.gateway;

import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeAnswerTaskBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.service.KnowledgeAnswerRunService;
import top.egon.cola.archetype.source.agent.domain.knowledge.service.KnowledgeQaEventObserverService;

/**
 * Domain-owned port for generating one answer from the references retrieval produced.
 *
 * <p>Keeps the model vendor out of the use cases, the way {@link KnowledgeVectorGateway} keeps the
 * vector engine out of it: the host names the logical model, the adapter resolves it and its
 * provider, and the use case only sees the event vocabulary. The call returns as soon as the stream
 * is established — the events arrive through the observer, and the returned handle ends the
 * generation early.
 */
public interface KnowledgeAnswerGateway {

    /**
     * Starts one generation and publishes its lifecycle through the observer: exactly one
     * {@code STARTED}, zero or more {@code PROGRESS}, and exactly one terminal event.
     *
     * @return handle that cancels the generation and releases the model subscription
     */
    KnowledgeAnswerRunService generate(KnowledgeAnswerTaskBO task, KnowledgeQaEventObserverService observer);
}
