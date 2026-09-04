package top.egon.cola.archetype.source.agent.domain.research.gateway;

import top.egon.cola.archetype.source.agent.domain.research.model.DeepResearchTaskBO;
import top.egon.cola.archetype.source.agent.domain.research.service.DeepResearchEventObserverService;
import top.egon.cola.archetype.source.agent.domain.research.service.DeepResearchRunService;

/** Domain-owned port for starting one fixed Deep Research execution. */
public interface DeepResearchAgentGateway {

    DeepResearchRunService start(DeepResearchTaskBO task, DeepResearchEventObserverService observer);
}
