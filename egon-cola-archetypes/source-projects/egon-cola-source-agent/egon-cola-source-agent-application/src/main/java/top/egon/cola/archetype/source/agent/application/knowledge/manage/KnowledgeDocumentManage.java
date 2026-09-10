package top.egon.cola.archetype.source.agent.application.knowledge.manage;

import jakarta.validation.Valid;
import top.egon.cola.archetype.source.agent.application.knowledge.command.UploadKnowledgeDocumentCommand;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.DocumentIngestStatusEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeDocumentBO;
import top.egon.cola.component.common.core.pojo.PageResultRecord;

/** Application facade over the document lifecycle of one knowledge base. */
public interface KnowledgeDocumentManage {

    /**
     * Stores the original, extracts its text and queues the ingest, all before the call returns.
     *
     * @return the stored document in its initial {@code PENDING} state; the chunks exist only after
     *         the queued delivery has run
     */
    KnowledgeDocumentBO upload(@Valid UploadKnowledgeDocumentCommand command);

    KnowledgeDocumentBO get(Long documentId);

    PageResultRecord<KnowledgeDocumentBO> page(Long knowledgeBaseId, int page, int size,
                                               DocumentIngestStatusEnum status, String keyword);

    /** Queues another ingest attempt for a document that reached a terminal status. */
    KnowledgeDocumentBO reingest(Long documentId);

    /** Removes the document, its stored original and its chunks. */
    void delete(Long documentId);
}
