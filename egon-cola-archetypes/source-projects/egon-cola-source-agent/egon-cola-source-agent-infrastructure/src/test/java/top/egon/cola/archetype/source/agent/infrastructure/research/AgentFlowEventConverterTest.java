package top.egon.cola.archetype.source.agent.infrastructure.research;

import com.google.adk.events.Event;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import org.junit.jupiter.api.Test;
import top.egon.cola.archetype.source.agent.domain.research.model.DeepResearchEvent;
import top.egon.cola.archetype.source.agent.domain.research.model.ResearchEventTypeEnum;
import top.egon.cola.archetype.source.agent.domain.research.model.ResearchStageEnum;
import top.egon.cola.archetype.source.agent.infrastructure.research.converter.AgentFlowEventConverter;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AgentFlowEventConverterTest {

    private static final Instant NOW = Instant.parse("2026-09-04T08:00:00Z");

    @Test
    void maps_only_allowlisted_text_and_author_fields() {
        Event source = Event.builder()
                .id("event-1")
                .author("EvidenceResearcher")
                .partial(true)
                .content(Content.fromParts(
                        Part.fromText("safe evidence"),
                        Part.fromFunctionCall("search", Map.of("apiKey", "do-not-forward"))))
                .timestamp(NOW.toEpochMilli())
                .build();

        DeepResearchEvent mapped = AgentFlowEventConverter.INSTANCE.toDomain(
                source, "run-1", 2, NOW, "trace-1");

        assertNotNull(mapped);
        assertEquals(ResearchEventTypeEnum.PROGRESS, mapped.type());
        assertEquals(ResearchStageEnum.EVIDENCE_RESEARCH, mapped.stage());
        assertEquals("EvidenceResearcher", mapped.agentName());
        assertEquals("safe evidence", mapped.delta());
        assertFalse(mapped.delta().contains("do-not-forward"));
    }

    @Test
    void maps_domain_event_back_to_minimal_adk_event_without_raw_metadata() {
        DeepResearchEvent source = DeepResearchEvent.completed(
                "run-1", 3, "# safe report", NOW, "trace-1");

        Event mapped = AgentFlowEventConverter.INSTANCE.toSource(source);

        assertEquals("run-1", mapped.id());
        assertEquals("# safe report", mapped.content().orElseThrow().text());
        assertEquals(NOW.toEpochMilli(), mapped.timestamp());
        assertEquals(0, mapped.functionCalls().size());
        assertEquals(0, mapped.functionResponses().size());
    }
}
