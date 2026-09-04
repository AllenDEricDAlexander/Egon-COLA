package top.egon.cola.archetype.source.agent.domain.research.service;

import top.egon.cola.archetype.source.agent.domain.research.model.DeepResearchEvent;

/** Callback port for non-blocking Deep Research lifecycle events. */
@FunctionalInterface
public interface DeepResearchEventObserverService {

    void onEvent(DeepResearchEvent event);
}
