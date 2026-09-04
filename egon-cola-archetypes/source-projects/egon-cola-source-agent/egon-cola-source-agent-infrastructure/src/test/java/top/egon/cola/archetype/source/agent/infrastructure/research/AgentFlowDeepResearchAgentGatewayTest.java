package top.egon.cola.archetype.source.agent.infrastructure.research;

import com.google.adk.events.Event;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.processors.PublishProcessor;
import org.junit.jupiter.api.Test;
import top.egon.cola.archetype.source.agent.domain.research.model.DeepResearchEvent;
import top.egon.cola.archetype.source.agent.domain.research.model.DeepResearchTaskBO;
import top.egon.cola.archetype.source.agent.domain.research.model.ReportLanguageEnum;
import top.egon.cola.archetype.source.agent.domain.research.service.DeepResearchEventObserverService;
import top.egon.cola.archetype.source.agent.domain.research.service.DeepResearchRunService;
import top.egon.cola.archetype.source.agent.infrastructure.research.gateway.AgentFlowDeepResearchAgentGateway;
import top.egon.cola.component.agentflow.api.AgentFlowExecutionCommand;
import top.egon.cola.component.agentflow.api.AgentFlowService;
import top.egon.cola.component.agentflow.api.AgentFlowSessionCommand;
import top.egon.cola.component.agentflow.api.AgentFlowSessionResult;
import top.egon.cola.archetype.source.agent.domain.research.service.DeepResearchRunService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentFlowDeepResearchAgentGatewayTest {

    private static final Instant NOW = Instant.parse("2026-09-04T08:00:00Z");

    @Test
    void creates_one_isolated_session_and_deletes_it_after_terminal_event() {
        AgentFlowService flow = mock(AgentFlowService.class);
        PublishProcessor<Event> events = PublishProcessor.create();
        when(flow.createSession(any(AgentFlowSessionCommand.class)))
                .thenReturn(new AgentFlowSessionResult("deep-research", "session-1", NOW));
        when(flow.executeStream(any(AgentFlowExecutionCommand.class))).thenReturn(events);
        AgentFlowDeepResearchAgentGateway gateway = gateway(flow);
        List<DeepResearchEvent> received = new ArrayList<>();

        DeepResearchRunService run = gateway.start(task(), received::add);
        events.onNext(progress("Planner", "planning"));
        events.onNext(completed("# report"));
        events.onComplete();

        assertEquals("run-1", run.runId());
        assertEquals(3, received.size());
        assertEquals(List.of(1L, 2L, 3L), received.stream().map(DeepResearchEvent::sequence).toList());
        assertEquals("STARTED", received.getFirst().type().name());
        assertEquals("COMPLETED", received.getLast().type().name());
        verify(flow).createSession(new AgentFlowSessionCommand("deep-research", "research-run-1", null));
        verify(flow).executeStream(any(AgentFlowExecutionCommand.class));
        verify(flow).deleteSession(new AgentFlowSessionCommand("deep-research", "research-run-1", "session-1"));
    }

    @Test
    void maps_stream_error_to_safe_failed_event_and_deletes_session_once() {
        AgentFlowService flow = mock(AgentFlowService.class);
        PublishProcessor<Event> events = PublishProcessor.create();
        when(flow.createSession(any(AgentFlowSessionCommand.class)))
                .thenReturn(new AgentFlowSessionResult("deep-research", "session-1", NOW));
        when(flow.executeStream(any(AgentFlowExecutionCommand.class))).thenReturn(events);
        AgentFlowDeepResearchAgentGateway gateway = gateway(flow);
        List<DeepResearchEvent> received = new ArrayList<>();

        gateway.start(task(), received::add);
        events.onError(new IllegalStateException("provider-secret-must-not-escape"));

        assertEquals("FAILED", received.getLast().type().name());
        assertEquals(2, received.getLast().sequence());
        assertEquals("research dependency is unavailable", received.getLast().errorMessage());
        assertFalse(received.getLast().errorMessage().contains("provider-secret"));
        verify(flow).deleteSession(new AgentFlowSessionCommand("deep-research", "research-run-1", "session-1"));
    }

    @Test
    void cancellation_disposes_stream_and_deletes_session_exactly_once() {
        AgentFlowService flow = mock(AgentFlowService.class);
        PublishProcessor<Event> events = PublishProcessor.create();
        when(flow.createSession(any(AgentFlowSessionCommand.class)))
                .thenReturn(new AgentFlowSessionResult("deep-research", "session-1", NOW));
        when(flow.executeStream(any(AgentFlowExecutionCommand.class))).thenReturn(events);
        AgentFlowDeepResearchAgentGateway gateway = gateway(flow);

        DeepResearchRunService run = gateway.start(task(), event -> { });
        run.cancel();
        run.cancel();
        events.onNext(progress("Planner", "late"));

        assertFalse(events.hasSubscribers());
        verify(flow).deleteSession(new AgentFlowSessionCommand("deep-research", "research-run-1", "session-1"));
    }

    @Test
    void session_creation_failure_does_not_attempt_delete() {
        AgentFlowService flow = mock(AgentFlowService.class);
        when(flow.createSession(any(AgentFlowSessionCommand.class)))
                .thenThrow(new IllegalStateException("not available"));
        AgentFlowDeepResearchAgentGateway gateway = gateway(flow);

        assertThrows(IllegalStateException.class, () -> gateway.start(task(), event -> { }));

        verify(flow).createSession(any(AgentFlowSessionCommand.class));
        verify(flow, never()).executeStream(any(AgentFlowExecutionCommand.class));
        verify(flow, never()).deleteSession(any(AgentFlowSessionCommand.class));
    }

    private static AgentFlowDeepResearchAgentGateway gateway(AgentFlowService flow) {
        return new AgentFlowDeepResearchAgentGateway(flow,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static DeepResearchTaskBO task() {
        return DeepResearchTaskBO.create("run-1", "agent architecture", ReportLanguageEnum.EN_US,
                8, NOW.plusSeconds(300), "trace-1");
    }

    private static Event progress(String author, String text) {
        return Event.builder().id("event-" + author).author(author).partial(true)
                .content(Content.fromParts(Part.fromText(text))).timestamp(NOW.toEpochMilli()).build();
    }

    private static Event completed(String report) {
        return Event.builder().id("event-writer").author("Writer")
                .content(Content.fromParts(Part.fromText(report))).timestamp(NOW.toEpochMilli()).build();
    }
}
