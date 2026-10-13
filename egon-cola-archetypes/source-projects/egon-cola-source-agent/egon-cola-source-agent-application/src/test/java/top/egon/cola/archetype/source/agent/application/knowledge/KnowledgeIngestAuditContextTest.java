package top.egon.cola.archetype.source.agent.application.knowledge;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import top.egon.cola.archetype.source.agent.application.knowledge.service.KnowledgeIngestQueueService;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeDocumentBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.repository.KnowledgeDocumentRepository;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.TransactionalOutbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The actual queue must retain trusted audit facts without changing its business payload. */
class KnowledgeIngestAuditContextTest {
    @AfterEach
    void clearScope() { MDC.clear(); }

    @Test
    void missingAndConflictingActorFactsStopBeforeEitherDocumentWrite() {
        var documents = mock(KnowledgeDocumentRepository.class);
        var outbox = mock(TransactionalOutbox.class);
        var queue = new KnowledgeIngestQueueService(documents, outbox,
                new top.egon.cola.component.common.mybatis.business.EgonColaMdcUserIdProvider(
                        new top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties()));
        var document = KnowledgeDocumentBO.create(1001L, 2001L, "notes", "notes.txt", "text/plain",
                5, "a".repeat(64), "LOCAL", "notes.txt", "notes");
        MDC.put("tenantId", "1001");
        org.junit.jupiter.api.Assertions.assertThrows(top.egon.cola.archetype.source.agent.common.exception.KnowledgeApplicationException.class,
                () -> queue.storeAndEnqueue(document, "trace-1"));
        MDC.put("userId", "subject-1001"); MDC.put("egon.auth.actor-kind", "SYSTEM");
        org.junit.jupiter.api.Assertions.assertThrows(top.egon.cola.archetype.source.agent.common.exception.KnowledgeApplicationException.class,
                () -> queue.storeAndEnqueue(document, "trace-1"));
        MDC.put("egon.auth.actor-kind", "USER"); MDC.put("tenantId", "1002");
        org.junit.jupiter.api.Assertions.assertThrows(top.egon.cola.archetype.source.agent.common.exception.KnowledgeApplicationException.class,
                () -> queue.resetAndEnqueue(document, "trace-1"));
        org.mockito.Mockito.verifyNoInteractions(documents, outbox);
    }

    @Test
    void writesProtectedActorFacts() {
        var documents = mock(KnowledgeDocumentRepository.class);
        var outbox = mock(TransactionalOutbox.class);
        var queue = new KnowledgeIngestQueueService(documents, outbox,
                new top.egon.cola.component.common.mybatis.business.EgonColaMdcUserIdProvider(
                        new top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties()));
        var document = new KnowledgeDocumentBO(3001L, 1001L, 2001L, "notes", "notes.txt", "text/plain",
                5, "a".repeat(64), "LOCAL", "notes.txt", "notes",
                top.egon.cola.archetype.source.agent.domain.knowledge.model.DocumentIngestStatusEnum.PENDING,
                0, 0, null, null, null, null);
        MDC.put("tenantId", "1001");
        MDC.put("userId", "subject-1001");
        MDC.put("egon.auth.actor-kind", "USER");
        when(documents.resetForReingest(document.documentId())).thenReturn(true);
        assertTrue(queue.resetAndEnqueue(document, "trace-1"));
        var message = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outbox).enqueue(message.capture());
        assertEquals(Map.of("egon.auth.actor-id", "subject-1001", "egon.auth.actor-kind", "USER",
                "egon.auth.tenant-id", "1001"), message.getValue().headers());
        assertEquals(Map.of("schemaVersion", "1", "documentId", document.documentId(), "tenantId", 1001L),
                message.getValue().payload());
    }
}
