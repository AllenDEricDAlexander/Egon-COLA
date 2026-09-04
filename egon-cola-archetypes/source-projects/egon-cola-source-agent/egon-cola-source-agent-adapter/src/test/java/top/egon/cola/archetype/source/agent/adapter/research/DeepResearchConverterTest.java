package top.egon.cola.archetype.source.agent.adapter.research;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import top.egon.cola.archetype.source.agent.adapter.handler.DeepResearchErrorResponse;
import top.egon.cola.archetype.source.agent.adapter.research.converter.DeepResearchCommandConverter;
import top.egon.cola.archetype.source.agent.adapter.research.converter.DeepResearchErrorConverter;
import top.egon.cola.archetype.source.agent.adapter.research.converter.ResearchEventConverter;
import top.egon.cola.archetype.source.agent.adapter.research.dto.StartDeepResearchRequest;
import top.egon.cola.archetype.source.agent.adapter.research.vo.DeepResearchEventVO;
import top.egon.cola.archetype.source.agent.application.research.command.StartDeepResearchCommand;
import top.egon.cola.archetype.source.agent.application.research.exception.DeepResearchApplicationException;
import top.egon.cola.archetype.source.agent.common.error.ResearchErrorCodeEnum;
import top.egon.cola.archetype.source.agent.domain.research.model.DeepResearchEvent;
import top.egon.cola.archetype.source.agent.domain.research.model.ReportLanguageEnum;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeepResearchConverterTest {

    private static final Instant NOW = Instant.parse("2026-09-04T08:00:00Z");

    @Test
    void maps_request_defaults_and_preserves_public_command_fields() {
        StartDeepResearchRequest request = new StartDeepResearchRequest(" topic ", null, null);

        StartDeepResearchCommand command = DeepResearchCommandConverter.INSTANCE.toCommand(request, "trace-1");
        StartDeepResearchRequest roundTrip = DeepResearchCommandConverter.INSTANCE.toSource(command);

        assertEquals("topic", command.topic());
        assertEquals(ReportLanguageEnum.ZH_CN, command.reportLanguage());
        assertEquals(8, command.maxSources());
        assertEquals("trace-1", command.traceId());
        assertEquals("topic", roundTrip.topic());
        assertEquals(ReportLanguageEnum.ZH_CN, roundTrip.reportLanguage());
        assertEquals(8, roundTrip.maxSources());
    }

    @Test
    void maps_error_without_cause_and_event_with_conditional_fields() {
        DeepResearchApplicationException failure = new DeepResearchApplicationException(
                ResearchErrorCodeEnum.RESEARCH_DEPENDENCY_UNAVAILABLE, "trace-1",
                new IllegalStateException("secret-provider-message"));

        DeepResearchErrorResponse error = DeepResearchErrorConverter.INSTANCE.toResponse(failure, NOW);
        DeepResearchApplicationException restored = DeepResearchErrorConverter.INSTANCE.toSource(error);
        DeepResearchEvent completed = DeepResearchEvent.completed("run-1", 2, "# report", NOW, "trace-1");
        DeepResearchEventVO event = ResearchEventConverter.INSTANCE.toTarget(completed);

        assertEquals("RESEARCH_DEPENDENCY_UNAVAILABLE", error.code());
        assertEquals("research dependency is unavailable", error.message());
        assertEquals(java.util.Map.of(), error.fieldErrors());
        assertEquals(failure.code(), restored.code());
        assertEquals("# report", event.reportMarkdown());
        assertNull(event.delta());
        assertEquals(completed, ResearchEventConverter.INSTANCE.toSource(event));
    }

    @Test
    void distinguishes_absent_optional_values_from_explicit_null_and_unknown_fields() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        StartDeepResearchRequest absent = mapper.readValue(
                "{\"topic\":\"agent architecture\"}", StartDeepResearchRequest.class);

        assertNull(absent.reportLanguage());
        assertNull(absent.maxSources());
        assertThrows(Exception.class, () -> mapper.readValue(
                "{\"topic\":\"agent architecture\",\"maxSources\":null}", StartDeepResearchRequest.class));
        assertThrows(Exception.class, () -> mapper.readValue(
                "{\"topic\":\"agent architecture\",\"unknown\":true}", StartDeepResearchRequest.class));
    }
}
