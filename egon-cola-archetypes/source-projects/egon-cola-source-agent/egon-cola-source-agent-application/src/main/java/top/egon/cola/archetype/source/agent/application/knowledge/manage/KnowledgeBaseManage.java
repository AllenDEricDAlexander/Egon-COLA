package top.egon.cola.archetype.source.agent.application.knowledge.manage;

import jakarta.validation.Valid;
import top.egon.cola.archetype.source.agent.application.knowledge.command.CreateKnowledgeBaseCommand;
import top.egon.cola.archetype.source.agent.application.knowledge.command.UpdateKnowledgeBaseCommand;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeBaseBO;
import top.egon.cola.component.common.core.pojo.PageResultRecord;

/**
 * Application facade over the knowledge base lifecycle.
 *
 * <p>The identifier-taking methods take no trace id: a failure on a read or a delete has nothing to
 * echo, and the request boundary adds the correlation id the response needs.
 */
public interface KnowledgeBaseManage {

    KnowledgeBaseBO create(@Valid CreateKnowledgeBaseCommand command);

    KnowledgeBaseBO get(Long knowledgeBaseId);

    PageResultRecord<KnowledgeBaseBO> page(int page, int size, String keyword, String embeddingModel);

    KnowledgeBaseBO update(@Valid UpdateKnowledgeBaseCommand command);

    /**
     * Undeleted document count of one base, which the published representations carry beside it.
     *
     * <p>The count is requested per base rather than derived from a listing, so a caller renders the
     * same number whether it asked for one base or a page of them.
     */
    long documentCount(Long knowledgeBaseId);

    /** Removes the base, its documents, their stored originals and their chunks. */
    void delete(Long knowledgeBaseId);
}
