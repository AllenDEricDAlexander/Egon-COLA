package top.egon.cola.archetype.source.agent.application.research.manage;

import top.egon.cola.archetype.source.agent.application.research.command.StartDeepResearchCommand;
import top.egon.cola.archetype.source.agent.domain.research.service.DeepResearchEventObserverService;
import top.egon.cola.archetype.source.agent.domain.research.service.DeepResearchRunService;

/** Application facade for starting one bounded Deep Research run. */
public interface DeepResearchManage {

    DeepResearchRunService startResearch(
            StartDeepResearchCommand command, DeepResearchEventObserverService observer);
}
