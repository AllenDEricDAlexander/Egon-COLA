package top.egon.cola.archetype.source.agent.domain.knowledge;

import org.junit.jupiter.api.Test;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.ChunkingStrategyEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.DocumentIngestStatusEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeBaseBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeBaseStatusEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeChunkBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeChunkConfigBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeDocumentBO;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeDomainTest {

    private static final String CONTENT_HASH = "a4e0f5f2c1b9d7e3a8c6b4d2e0f1a3c5b7d9e1f3a5c7b9d1e3f5a7c9b1d3e5f7";

    private static final KnowledgeChunkConfigBO TOKEN_CONFIG =
            new KnowledgeChunkConfigBO(512, 64, 1, null);

    @Test
    void keeps_document_status_transitions_legal() {
        assertTrue(DocumentIngestStatusEnum.PENDING.canTransitionTo(DocumentIngestStatusEnum.PROCESSING));
        assertFalse(DocumentIngestStatusEnum.PENDING.canTransitionTo(DocumentIngestStatusEnum.SUCCEEDED));
        assertTrue(DocumentIngestStatusEnum.PROCESSING.canTransitionTo(DocumentIngestStatusEnum.SUCCEEDED));
        assertTrue(DocumentIngestStatusEnum.PROCESSING.canTransitionTo(DocumentIngestStatusEnum.PENDING));
        assertTrue(DocumentIngestStatusEnum.PROCESSING.canTransitionTo(DocumentIngestStatusEnum.DEAD));
        assertFalse(DocumentIngestStatusEnum.PROCESSING.canTransitionTo(DocumentIngestStatusEnum.FAILED));
        assertFalse(DocumentIngestStatusEnum.SUCCEEDED.canTransitionTo(DocumentIngestStatusEnum.PROCESSING));
        assertTrue(DocumentIngestStatusEnum.SUCCEEDED.canTransitionTo(DocumentIngestStatusEnum.PENDING));

        assertFalse(DocumentIngestStatusEnum.PENDING.canTransitionTo(DocumentIngestStatusEnum.PENDING));
        assertFalse(DocumentIngestStatusEnum.PENDING.canTransitionTo(null));

        assertTrue(DocumentIngestStatusEnum.FAILED.isTerminal());
        assertTrue(DocumentIngestStatusEnum.DEAD.isTerminal());
        assertFalse(DocumentIngestStatusEnum.PROCESSING.isTerminal());
    }

    @Test
    void normalizes_identifiers() {
        KnowledgeBaseBO base = KnowledgeBaseBO.create(" product-docs ", " 产品文档库 ", " 内部手册 ",
                " openai-small ", ChunkingStrategyEnum.TOKEN, TOKEN_CONFIG, 0L);

        assertNull(base.knowledgeBaseId());
        assertEquals(Long.valueOf(0L), base.tenantId());
        assertEquals("product-docs", base.code());
        assertEquals("产品文档库", base.name());
        assertEquals("内部手册", base.description());
        assertEquals("openai-small", base.embeddingModel());
        assertEquals(KnowledgeBaseStatusEnum.ACTIVE, base.status());
        assertNull(base.createdAt());

        assertNull(KnowledgeBaseBO.create("docs", "名称", "   ", "openai-small",
                ChunkingStrategyEnum.TOKEN, TOKEN_CONFIG, 0L).description());

        KnowledgeDocumentBO document = KnowledgeDocumentBO.create(0L, 7L, " report.pdf ", " report.pdf ",
                " application/pdf ", 182734L, CONTENT_HASH, "LOCAL", "0/7/report.pdf", "抽取文本");
        assertNull(document.documentId());
        assertEquals("report.pdf", document.displayName());
        assertEquals(DocumentIngestStatusEnum.PENDING, document.status());
        assertEquals(0, document.chunkCount());
        assertEquals(0, document.attemptCount());
        assertNull(document.errorCode());
        assertNull(document.errorMessage());

        assertThrows(IllegalArgumentException.class, () -> KnowledgeBaseBO.create("a", "名称", null,
                "openai-small", ChunkingStrategyEnum.TOKEN, TOKEN_CONFIG, 0L));
        assertThrows(IllegalArgumentException.class, () -> KnowledgeBaseBO.create("bad code", "名称", null,
                "openai-small", ChunkingStrategyEnum.TOKEN, TOKEN_CONFIG, 0L));
        assertThrows(IllegalArgumentException.class, () -> KnowledgeBaseBO.create("docs", "  ", null,
                "openai-small", ChunkingStrategyEnum.TOKEN, TOKEN_CONFIG, 0L));
        assertThrows(IllegalArgumentException.class, () -> KnowledgeBaseBO.create("docs", "名称", null,
                "openai-small", ChunkingStrategyEnum.TOKEN, TOKEN_CONFIG, -1L));
        assertThrows(IllegalArgumentException.class, () -> KnowledgeDocumentBO.create(0L, 7L, "report.pdf",
                "report.pdf", "application/pdf", -1L, CONTENT_HASH, "LOCAL", "0/7/report.pdf", "文本"));
        assertThrows(IllegalArgumentException.class, () -> KnowledgeDocumentBO.create(0L, 7L, "report.pdf",
                "report.pdf", "application/pdf", 1L, CONTENT_HASH.toUpperCase(), "LOCAL",
                "0/7/report.pdf", "文本"));
        assertThrows(IllegalArgumentException.class, () -> new KnowledgeChunkBO(7L, -1, "文本", 0.5));
        assertThrows(IllegalArgumentException.class, () -> new KnowledgeChunkBO(null, 0, "文本", null));
    }

    @Test
    void rejects_unknown_status() {
        assertEquals(Set.of("PENDING", "PROCESSING", "SUCCEEDED", "FAILED", "DEAD"),
                names(DocumentIngestStatusEnum.values()));
        assertEquals(Set.of("ACTIVE", "DELETED"), names(KnowledgeBaseStatusEnum.values()));
        assertEquals(Set.of("TOKEN", "MARKDOWN_HEADING", "RECURSIVE"),
                names(ChunkingStrategyEnum.values()));

        assertThrows(IllegalArgumentException.class, () -> DocumentIngestStatusEnum.valueOf("QUEUED"));
        assertThrows(IllegalArgumentException.class, () -> ChunkingStrategyEnum.valueOf("SEMANTIC"));

        assertThrows(IllegalArgumentException.class, () -> KnowledgeBaseBO.create("docs", "名称", null,
                "openai-small", ChunkingStrategyEnum.TOKEN,
                new KnowledgeChunkConfigBO(512, 64, 1, List.of(1, 2)), 0L));
        assertThrows(IllegalArgumentException.class, () -> new KnowledgeChunkConfigBO(64, 64, 1, null));
        assertThrows(IllegalArgumentException.class, () -> new KnowledgeChunkConfigBO(8192, 0, 1, null));
        assertThrows(IllegalArgumentException.class, () -> new KnowledgeChunkConfigBO(512, 0, 1, List.of(7)));

        assertEquals(List.of(1, 3), new KnowledgeChunkConfigBO(512, 0, 1, List.of(3, 1, 3)).headingLevels());
    }

    private static Set<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).collect(Collectors.toUnmodifiableSet());
    }
}
