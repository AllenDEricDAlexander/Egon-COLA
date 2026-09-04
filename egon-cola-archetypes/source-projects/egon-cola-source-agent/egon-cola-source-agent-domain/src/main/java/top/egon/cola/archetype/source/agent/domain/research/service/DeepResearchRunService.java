package top.egon.cola.archetype.source.agent.domain.research.service;

/** Cancellable handle for one process-local Deep Research run. */
public interface DeepResearchRunService {

    String runId();

    void cancel();
}
