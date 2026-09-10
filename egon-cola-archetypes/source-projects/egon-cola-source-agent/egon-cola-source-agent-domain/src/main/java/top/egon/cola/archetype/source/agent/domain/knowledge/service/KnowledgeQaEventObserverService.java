package top.egon.cola.archetype.source.agent.domain.knowledge.service;

import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeQaEvent;

/** Callback port for non-blocking knowledge question and answer lifecycle events. */
@FunctionalInterface
public interface KnowledgeQaEventObserverService {

    void onEvent(KnowledgeQaEvent event);
}
