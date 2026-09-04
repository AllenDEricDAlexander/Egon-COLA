package top.egon.cola.archetype.source.agent.infrastructure.research.gateway;

import com.google.adk.events.Event;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import top.egon.cola.archetype.source.agent.common.error.ResearchErrorCodeEnum;
import top.egon.cola.archetype.source.agent.domain.research.gateway.DeepResearchAgentGateway;
import top.egon.cola.archetype.source.agent.domain.research.model.DeepResearchEvent;
import top.egon.cola.archetype.source.agent.domain.research.model.DeepResearchTaskBO;
import top.egon.cola.archetype.source.agent.domain.research.service.DeepResearchEventObserverService;
import top.egon.cola.archetype.source.agent.domain.research.service.DeepResearchRunService;
import top.egon.cola.archetype.source.agent.infrastructure.research.converter.AgentFlowEventConverter;
import top.egon.cola.component.agentflow.api.AgentFlowExecutionCommand;
import top.egon.cola.component.agentflow.api.AgentFlowService;
import top.egon.cola.component.agentflow.api.AgentFlowSessionCommand;
import top.egon.cola.component.agentflow.api.AgentFlowSessionResult;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Infrastructure Adapter that owns one Agent Flow session and its stream cleanup. */
@Service("deepResearchAgentGateway")
@RequiredArgsConstructor
@Slf4j
public class AgentFlowDeepResearchAgentGateway implements DeepResearchAgentGateway {

    private static final String FLOW_ID = "deep-research";

    @Qualifier("agentFlowService")
    private final AgentFlowService agentFlowService;

    @Qualifier("agentFlowClock")
    private final Clock clock;

    @Override
    public DeepResearchRunService start(DeepResearchTaskBO task, DeepResearchEventObserverService observer) {
        Objects.requireNonNull(task, "task must not be null");
        Objects.requireNonNull(observer, "observer must not be null");
        String userId = "research-" + task.runId();
        AgentFlowSessionResult session = createSession(userId);
        AtomicBoolean terminal = new AtomicBoolean();
        AtomicBoolean cleaned = new AtomicBoolean();
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicLong sequence = new AtomicLong(2);
        AtomicReference<Disposable> subscription = new AtomicReference<>();

        Runnable cleanup = () -> cleanup(session, userId, cleaned, subscription);
        try {
            observer.onEvent(DeepResearchEvent.started(task.runId(), task.traceId(), clock.instant()));
            Flowable<Event> stream = agentFlowService.executeStream(new AgentFlowExecutionCommand(
                    FLOW_ID, userId, session.sessionId(), message(task)));
            if (stream == null) {
                throw new IllegalStateException("Agent Flow returned no execution stream");
            }
            Disposable disposable = stream.subscribe(
                    event -> onEvent(task, observer, event, terminal, sequence, cleanup),
                    failure -> onFailure(task, observer, failure, terminal, sequence, cleanup),
                    () -> onComplete(task, observer, terminal, sequence, cleanup));
            subscription.set(disposable);
            if (cleaned.get()) {
                disposable.dispose();
            }
            return new GatewayRun(task.runId(), terminal, cancelled, subscription, cleanup);
        } catch (RuntimeException failure) {
            cleanup.run();
            throw failure;
        }
    }

    private AgentFlowSessionResult createSession(String userId) {
        try {
            AgentFlowSessionResult result = agentFlowService.createSession(
                    new AgentFlowSessionCommand(FLOW_ID, userId, null));
            return Objects.requireNonNull(result, "Agent Flow returned no session");
        } catch (RuntimeException failure) {
            throw new IllegalStateException("Agent Flow session creation failed", failure);
        }
    }

    private void onEvent(DeepResearchTaskBO task, DeepResearchEventObserverService observer, Event source,
                         AtomicBoolean terminal, AtomicLong sequence, Runnable cleanup) {
        if (terminal.get()) {
            return;
        }
        try {
            DeepResearchEvent mapped = AgentFlowEventConverter.INSTANCE.toDomain(
                    source, task.runId(), sequence.get(), clock.instant(), task.traceId());
            if (mapped == null) {
                return;
            }
            sequence.incrementAndGet();
            if (mapped.isTerminal() && !terminal.compareAndSet(false, true)) {
                return;
            }
            observer.onEvent(mapped);
            if (mapped.isTerminal()) {
                cleanup.run();
            }
        } catch (RuntimeException failure) {
            onFailure(task, observer, failure, terminal, sequence, cleanup);
        }
    }

    private void onFailure(DeepResearchTaskBO task, DeepResearchEventObserverService observer, Throwable failure,
                           AtomicBoolean terminal, AtomicLong sequence, Runnable cleanup) {
        if (terminal.compareAndSet(false, true)) {
            ResearchErrorCodeEnum code = isTimeout(failure)
                    ? ResearchErrorCodeEnum.RESEARCH_TIMEOUT
                    : ResearchErrorCodeEnum.RESEARCH_DEPENDENCY_UNAVAILABLE;
            try {
                observer.onEvent(DeepResearchEvent.failed(task.runId(), sequence.getAndIncrement(), code,
                        clock.instant(), task.traceId()));
            } catch (RuntimeException observerFailure) {
                log.warn("Deep Research gateway runId={} traceId={} stage=STREAM outcome=OBSERVER_ERROR errorType={}",
                        task.runId(), task.traceId(), observerFailure.getClass().getSimpleName());
            }
        }
        cleanup.run();
    }

    private void onComplete(DeepResearchTaskBO task, DeepResearchEventObserverService observer,
                            AtomicBoolean terminal, AtomicLong sequence, Runnable cleanup) {
        if (terminal.compareAndSet(false, true)) {
            try {
                observer.onEvent(DeepResearchEvent.failed(task.runId(), sequence.getAndIncrement(),
                        ResearchErrorCodeEnum.RESEARCH_INTERNAL_ERROR, clock.instant(), task.traceId()));
            } catch (RuntimeException observerFailure) {
                log.warn("Deep Research gateway runId={} traceId={} stage=STREAM outcome=OBSERVER_ERROR errorType={}",
                        task.runId(), task.traceId(), observerFailure.getClass().getSimpleName());
            }
        }
        cleanup.run();
    }

    private void cleanup(AgentFlowSessionResult session, String userId, AtomicBoolean cleaned,
                         AtomicReference<Disposable> subscription) {
        if (!cleaned.compareAndSet(false, true)) {
            return;
        }
        Disposable disposable = subscription.get();
        if (disposable != null) {
            disposable.dispose();
        }
        try {
            agentFlowService.deleteSession(new AgentFlowSessionCommand(FLOW_ID, userId, session.sessionId()));
        } catch (RuntimeException failure) {
            log.warn("Deep Research gateway stage=SESSION outcome=DELETE_ERROR errorType={}",
                    failure.getClass().getSimpleName());
        }
    }

    private static Content message(DeepResearchTaskBO task) {
        String prompt = "Research the following topic using the configured evidence tools. "
                + "Return a source-grounded report in " + task.language().wireValue() + ". "
                + "Use at most " + task.maxSources() + " sources. Topic: " + task.topic().value();
        return Content.fromParts(Part.fromText(prompt));
    }

    private static boolean isTimeout(Throwable failure) {
        return failure instanceof TimeoutException
                || failure.getClass().getSimpleName().toLowerCase().contains("timeout");
    }

    private static final class GatewayRun implements DeepResearchRunService {
        private final String runId;
        private final AtomicBoolean terminal;
        private final AtomicBoolean cancelled;
        private final AtomicReference<Disposable> subscription;
        private final Runnable cleanup;

        private GatewayRun(String runId, AtomicBoolean terminal, AtomicBoolean cancelled,
                           AtomicReference<Disposable> subscription, Runnable cleanup) {
            this.runId = runId;
            this.terminal = terminal;
            this.cancelled = cancelled;
            this.subscription = subscription;
            this.cleanup = cleanup;
        }

        @Override
        public String runId() {
            return runId;
        }

        @Override
        public void cancel() {
            if (!cancelled.compareAndSet(false, true)) {
                return;
            }
            terminal.set(true);
            Disposable disposable = subscription.get();
            if (disposable != null) {
                disposable.dispose();
            }
            cleanup.run();
        }
    }
}
