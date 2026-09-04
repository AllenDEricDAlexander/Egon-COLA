package top.egon.cola.archetype.source.agent.domain.research;

import org.junit.jupiter.api.Test;
import top.egon.cola.archetype.source.agent.common.error.ResearchErrorCodeEnum;
import top.egon.cola.archetype.source.agent.domain.research.model.DeepResearchEvent;
import top.egon.cola.archetype.source.agent.domain.research.model.DeepResearchTaskBO;
import top.egon.cola.archetype.source.agent.domain.research.model.ResearchEventTypeEnum;
import top.egon.cola.archetype.source.agent.domain.research.model.ResearchStageEnum;
import top.egon.cola.archetype.source.agent.domain.research.model.ResearchTopicBO;
import top.egon.cola.archetype.source.agent.domain.research.model.ReportLanguageEnum;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeepResearchDomainTest {

    private static final Instant NOW = Instant.parse("2026-09-04T08:00:00Z");

    @Test
    void normalizes_topic_and_rejects_blank_control_or_oversized_values() {
        assertEquals("agent architecture", ResearchTopicBO.create("  agent architecture  ").value());
        assertThrows(IllegalArgumentException.class, () -> ResearchTopicBO.create("  "));
        assertThrows(IllegalArgumentException.class, () -> ResearchTopicBO.create("line\nfeed"));
        assertThrows(IllegalArgumentException.class, () -> ResearchTopicBO.create("x".repeat(501)));
    }

    @Test
    void creates_bounded_immutable_task_values() {
        String runId = UUID.randomUUID().toString();
        DeepResearchTaskBO task = DeepResearchTaskBO.create(
                runId, "  topic  ", ReportLanguageEnum.ZH_CN, 8, NOW.plusSeconds(300), "trace-1");

        assertEquals(runId, task.runId());
        assertEquals("topic", task.topic().value());
        assertEquals(ReportLanguageEnum.ZH_CN, task.language());
        assertEquals(8, task.maxSources());
        assertEquals(NOW.plusSeconds(300), task.deadline());
        assertEquals("trace-1", task.traceId());
    }

    @Test
    void enforces_conditional_event_payloads_and_terminal_types() {
        String runId = UUID.randomUUID().toString();
        DeepResearchEvent started = DeepResearchEvent.started(runId, "trace-1", NOW);
        DeepResearchEvent progress = DeepResearchEvent.progress(runId, 2, ResearchStageEnum.EVIDENCE_RESEARCH,
                "EvidenceResearcher", "checking sources", NOW.plusMillis(1), "trace-1");
        DeepResearchEvent completed = DeepResearchEvent.completed(runId, 3, "# report", NOW.plusMillis(2), "trace-1");
        DeepResearchEvent failed = DeepResearchEvent.failed(runId, 4, ResearchErrorCodeEnum.RESEARCH_TIMEOUT,
                NOW.plusMillis(3), "trace-1");

        assertEquals(ResearchEventTypeEnum.STARTED, started.type());
        assertEquals(ResearchEventTypeEnum.PROGRESS, progress.type());
        assertEquals(ResearchEventTypeEnum.COMPLETED, completed.type());
        assertEquals(ResearchEventTypeEnum.FAILED, failed.type());
        assertTrue(completed.isTerminal());
        assertTrue(failed.isTerminal());
        assertThrows(IllegalArgumentException.class, () -> new DeepResearchEvent(runId, 5, ResearchEventTypeEnum.COMPLETED,
                ResearchStageEnum.COMPLETED, NOW, "trace-1", null, null, null,
                ResearchErrorCodeEnum.RESEARCH_TIMEOUT, "bad", true));
    }
}
