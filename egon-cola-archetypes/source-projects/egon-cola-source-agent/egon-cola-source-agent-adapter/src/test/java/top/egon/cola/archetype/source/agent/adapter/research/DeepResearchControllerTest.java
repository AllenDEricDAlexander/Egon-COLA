package top.egon.cola.archetype.source.agent.adapter.research;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.egon.cola.archetype.source.agent.adapter.config.DeepResearchApiProperties;
import top.egon.cola.archetype.source.agent.adapter.filter.ResearchApiKeyFilter;
import top.egon.cola.archetype.source.agent.adapter.filter.ResearchTraceFilter;
import top.egon.cola.archetype.source.agent.adapter.handler.DeepResearchGlobalExceptionHandler;
import top.egon.cola.archetype.source.agent.adapter.research.controller.DeepResearchController;
import top.egon.cola.archetype.source.agent.application.research.command.StartDeepResearchCommand;
import top.egon.cola.archetype.source.agent.application.research.config.DeepResearchRuntimeProperties;
import top.egon.cola.archetype.source.agent.application.research.exception.DeepResearchApplicationException;
import top.egon.cola.archetype.source.agent.application.research.manage.DeepResearchManage;
import top.egon.cola.archetype.source.agent.common.error.ResearchErrorCodeEnum;
import top.egon.cola.archetype.source.agent.domain.research.model.DeepResearchEvent;
import top.egon.cola.archetype.source.agent.domain.research.model.ReportLanguageEnum;
import top.egon.cola.archetype.source.agent.domain.research.service.DeepResearchEventObserverService;
import top.egon.cola.archetype.source.agent.domain.research.service.DeepResearchRunService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeepResearchControllerTest {

    private static final Instant NOW = Instant.parse("2026-09-04T08:00:00Z");

    @Test
    void accepts_one_post_sse_command_and_emits_one_terminal_event() throws Exception {
        DeepResearchManage manage = mock(DeepResearchManage.class);
        RecordingRun run = new RecordingRun("run-1");
        when(manage.startResearch(any(StartDeepResearchCommand.class), any(DeepResearchEventObserverService.class)))
                .thenAnswer(invocation -> {
                    DeepResearchEventObserverService observer = invocation.getArgument(1);
                    observer.onEvent(DeepResearchEvent.started("run-1", "trace-1", NOW));
                    observer.onEvent(DeepResearchEvent.completed("run-1", 2, "# report", NOW, "trace-1"));
                    return run;
                });
        var mvc = MockMvcBuilders.standaloneSetup(new DeepResearchController(manage,
                        new DeepResearchRuntimeProperties(4, Duration.ofMinutes(5), 20, Duration.ofSeconds(15)),
                        Clock.fixed(NOW, ZoneOffset.UTC)))
                .addFilters(new ResearchTraceFilter(Clock.fixed(NOW, ZoneOffset.UTC),
                                new com.fasterxml.jackson.databind.ObjectMapper()
                                        .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())),
                        new ResearchApiKeyFilter(new DeepResearchApiProperties("test-key"),
                                new com.fasterxml.jackson.databind.ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC)))
                .setControllerAdvice(new DeepResearchGlobalExceptionHandler(Clock.fixed(NOW, ZoneOffset.UTC)))
                .build();

        MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/api/v1/deep-research/runs")
                        .header(ResearchApiKeyFilter.API_KEY_HEADER, "test-key")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"agent architecture\"}"))
                .andReturn();
        if (result.getRequest().isAsyncStarted()) {
            result = mvc.perform(MockMvcRequestBuilders.asyncDispatch(result)).andReturn();
        }

        assertTrue(result.getResponse().getContentAsString().contains("research.started"));
        assertTrue(result.getResponse().getContentAsString().contains("research.completed"));
        assertTrue(result.getResponse().getContentAsString().contains("run-1:2"));
        verify(manage).startResearch(any(StartDeepResearchCommand.class), any(DeepResearchEventObserverService.class));
    }

    @Test
    void maps_capacity_failure_before_stream_to_429() throws Exception {
        var response = new DeepResearchGlobalExceptionHandler(Clock.fixed(NOW, ZoneOffset.UTC))
                .handleApplication(new DeepResearchApplicationException(
                        ResearchErrorCodeEnum.RESEARCH_CAPACITY_EXHAUSTED, "trace-1"),
                        new MockHttpServletRequest());

        org.junit.jupiter.api.Assertions.assertEquals(429, response.getStatusCode().value());
        org.junit.jupiter.api.Assertions.assertEquals("5", response.getHeaders().getFirst("Retry-After"));
    }

    private static final class RecordingRun implements DeepResearchRunService {
        private final String runId;

        private RecordingRun(String runId) {
            this.runId = runId;
        }

        @Override
        public String runId() {
            return runId;
        }

        @Override
        public void cancel() {
        }
    }
}
