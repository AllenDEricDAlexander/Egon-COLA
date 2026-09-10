package top.egon.cola.archetype.source.agent.application.knowledge.manage;

import jakarta.validation.Valid;
import top.egon.cola.archetype.source.agent.application.knowledge.command.AskKnowledgeBaseCommand;
import top.egon.cola.archetype.source.agent.application.knowledge.command.RetrieveKnowledgeCommand;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeRetrievalBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.service.KnowledgeAnswerRunService;
import top.egon.cola.archetype.source.agent.domain.knowledge.service.KnowledgeQaEventObserverService;

/** Application facade over retrieval debugging and the retrieval-backed answer stream. */
public interface KnowledgeQaManage {

    /** Runs one retrieval and returns the scored chunks; no model is called. */
    KnowledgeRetrievalBO retrieve(@Valid RetrieveKnowledgeCommand command);

    /**
     * Retrieves, then streams one answer to the observer.
     *
     * @return the handle that cancels the generation; the permit this call acquired is released by
     *         the terminal event, by {@code cancel()} or by an observer failure, whichever is first
     */
    KnowledgeAnswerRunService ask(@Valid AskKnowledgeBaseCommand command,
                                  KnowledgeQaEventObserverService observer);
}
