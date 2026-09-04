package top.egon.cola.component.agentflow.execution;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.InvocationContext;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.agentflow.api.AgentFlowExecutionCommand;
import top.egon.cola.component.agentflow.api.AgentFlowSessionCommand;
import top.egon.cola.component.agentflow.api.AgentFlowSessionResult;
import top.egon.cola.component.agentflow.autoconfigure.AgentFlowProperties;
import top.egon.cola.component.agentflow.config.AgentFlowConfigDTO;
import top.egon.cola.component.agentflow.config.AgentConfigDTO;
import top.egon.cola.component.agentflow.exception.AgentFlowException;
import top.egon.cola.component.agentflow.exception.AgentFlowExecutionTimeoutException;
import top.egon.cola.component.agentflow.exception.AgentFlowSessionBusyException;
import top.egon.cola.component.agentflow.exception.AgentFlowSessionNotFoundException;
import top.egon.cola.component.agentflow.runtime.AgentFlowRuntimeBO;
import top.egon.cola.component.agentflow.runtime.DefaultAgentFlowRegistry;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultAgentFlowServiceTest {

    private static jakarta.validation.ValidatorFactory validatorFactory;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void creates_distinct_sessions_and_executes_ordered_events() {
        Event first = event("first");
        Event second = event("second");
        ControlledRunner runner = new ControlledRunner(Flowable.just(first, second));
        DefaultAgentFlowService service = service(runner, Duration.ofSeconds(1));

        AgentFlowSessionResult one = service.createSession(createCommand());
        AgentFlowSessionResult two = service.createSession(createCommand());
        List<Event> events = service.execute(new AgentFlowExecutionCommand(
                "flow", "user", one.sessionId(), Content.fromParts(Part.fromText("question"))));

        assertThat(one.sessionId()).isNotEqualTo(two.sessionId());
        assertThat(events).containsExactly(first, second);
        service.close();
    }

    @Test
    void executes_lazily_and_releases_busy_lease_after_error() {
        AtomicInteger subscriptions = new AtomicInteger();
        ControlledRunner runner = new ControlledRunner(Flowable.defer(() -> {
            subscriptions.incrementAndGet();
            return Flowable.error(new IllegalStateException("provider failure"));
        }));
        DefaultAgentFlowService service = service(runner, Duration.ofSeconds(1));
        AgentFlowSessionResult session = service.createSession(createCommand());
        AgentFlowExecutionCommand command = command(session.sessionId());

        Flowable<Event> stream = service.executeStream(command);
        assertThat(subscriptions).hasValue(0);
        assertThatThrownBy(() -> stream.blockingSubscribe())
                .hasCauseInstanceOf(IllegalStateException.class);
        assertThat(subscriptions).hasValue(1);
        assertThatThrownBy(() -> service.execute(command))
                .hasCauseInstanceOf(IllegalStateException.class)
                .isNotInstanceOf(AgentFlowSessionBusyException.class);
        service.close();
    }

    @Test
    void maps_missing_session_and_timeout_and_allows_cancelled_session_to_be_reused() {
        ControlledRunner runner = new ControlledRunner(Flowable.never());
        DefaultAgentFlowService service = service(runner, Duration.ofMillis(30));
        AgentFlowExecutionCommand missing = command("missing-session");

        assertThatThrownBy(() -> service.execute(missing))
                .isInstanceOf(AgentFlowSessionNotFoundException.class);

        AgentFlowSessionResult session = service.createSession(createCommand());
        AgentFlowExecutionCommand command = command(session.sessionId());
        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(AgentFlowExecutionTimeoutException.class)
                .hasCauseInstanceOf(java.util.concurrent.TimeoutException.class);

        service.close();
    }

    @Test
    void deletes_existing_session_and_rejects_it_afterwards() {
        ControlledRunner runner = new ControlledRunner(Flowable.empty());
        DefaultAgentFlowService service = service(runner, Duration.ofSeconds(1));
        AgentFlowSessionResult session = service.createSession(createCommand());

        service.deleteSession(new AgentFlowSessionCommand("flow", "user", session.sessionId()));
        assertThatThrownBy(() -> service.execute(command(session.sessionId())))
                .isInstanceOf(AgentFlowSessionNotFoundException.class);
        service.close();
    }

    @Test
    void returns_an_empty_immutable_sync_result() {
        ControlledRunner runner = new ControlledRunner(Flowable.empty());
        DefaultAgentFlowService service = service(runner, Duration.ofSeconds(1));
        AgentFlowSessionResult session = service.createSession(createCommand());

        List<Event> events = service.execute(command(session.sessionId()));

        assertThat(events).isEmpty();
        assertThatThrownBy(() -> events.add(event("unexpected")))
                .isInstanceOf(UnsupportedOperationException.class);
        service.close();
    }

    @Test
    void stream_is_lazy_and_each_subscription_runs_once() {
        AtomicInteger runs = new AtomicInteger();
        ControlledRunner runner = new ControlledRunner(() -> {
            runs.incrementAndGet();
            return Flowable.just(event("event-" + runs.get()));
        });
        DefaultAgentFlowService service = service(runner, Duration.ofSeconds(1));
        AgentFlowSessionResult session = service.createSession(createCommand());
        Flowable<Event> stream = service.executeStream(command(session.sessionId()));

        assertThat(runs).hasValue(0);
        assertThat(stream.toList().blockingGet()).extracting(Event::id).containsExactly("event-1");
        assertThat(stream.toList().blockingGet()).extracting(Event::id).containsExactly("event-2");
        assertThat(runs).hasValue(2);
        service.close();
    }

    @Test
    void cancellation_releases_busy_lease_and_allows_delete() {
        PublishProcessor<Event> processor = PublishProcessor.create();
        DefaultAgentFlowService service = service(new ControlledRunner(processor), Duration.ofSeconds(1));
        AgentFlowSessionResult session = service.createSession(createCommand());
        AgentFlowExecutionCommand command = command(session.sessionId());

        TestSubscriber<Event> active = service.executeStream(command).test();
        TestSubscriber<Event> busy = service.executeStream(command).test();
        busy.assertError(AgentFlowSessionBusyException.class);

        active.cancel();
        service.deleteSession(new AgentFlowSessionCommand("flow", "user", session.sessionId()));
        service.close();
    }

    @Test
    void timeout_disposes_upstream_and_allows_a_second_execution() {
        AtomicInteger runs = new AtomicInteger();
        AtomicBoolean disposed = new AtomicBoolean();
        ControlledRunner runner = new ControlledRunner(() -> {
            if (runs.getAndIncrement() == 0) {
                return Flowable.<Event>never().doOnCancel(() -> disposed.set(true));
            }
            return Flowable.just(event("after-timeout"));
        });
        DefaultAgentFlowService service = service(runner, Duration.ofMillis(30));
        AgentFlowSessionResult session = service.createSession(createCommand());
        AgentFlowExecutionCommand command = command(session.sessionId());

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(AgentFlowExecutionTimeoutException.class)
                .hasCauseInstanceOf(java.util.concurrent.TimeoutException.class);
        assertThat(disposed).isTrue();
        assertThat(service.execute(command)).extracting(Event::id).containsExactly("after-timeout");
        service.close();
    }

    @Test
    void rejects_delete_while_execution_is_active_and_closes_with_a_bound() {
        PublishProcessor<Event> processor = PublishProcessor.create();
        DefaultAgentFlowService service = service(new ControlledRunner(processor), Duration.ofMinutes(1),
                Duration.ofMillis(30));
        AgentFlowSessionResult session = service.createSession(createCommand());
        AgentFlowExecutionCommand command = command(session.sessionId());
        TestSubscriber<Event> active = service.executeStream(command).test();

        assertThatThrownBy(() -> service.deleteSession(
                new AgentFlowSessionCommand("flow", "user", session.sessionId())))
                .isInstanceOf(AgentFlowSessionBusyException.class);
        long started = System.nanoTime();
        service.close();
        Duration elapsed = Duration.ofNanos(System.nanoTime() - started);

        assertThat(elapsed).isLessThan(Duration.ofSeconds(1));
        assertThatThrownBy(service::listFlows).isInstanceOf(AgentFlowException.class);
        active.cancel();
    }

    @Test
    void validates_session_command_groups_before_registry_access() {
        DefaultAgentFlowService service = service(new ControlledRunner(Flowable.empty()), Duration.ofSeconds(1));

        assertThatThrownBy(() -> service.createSession(new AgentFlowSessionCommand(" ", "user", null)))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> service.deleteSession(new AgentFlowSessionCommand("flow", "user", " ")))
                .isInstanceOf(ConstraintViolationException.class);
        service.close();
    }

    private static DefaultAgentFlowService service(ControlledRunner runner, Duration timeout) {
        return service(runner, timeout, Duration.ofSeconds(1));
    }

    private static DefaultAgentFlowService service(
            ControlledRunner runner, Duration timeout, Duration shutdownTimeout) {
        AgentFlowRuntimeBO runtime = new AgentFlowRuntimeBO(
                "flow", "planner", "chatModel", "model-name", runner.agent(), runner,
                Instant.parse("2026-01-01T00:00:00Z"));
        DefaultAgentFlowRegistry registry = new DefaultAgentFlowRegistry(Map.of("flow", runtime), shutdownTimeout);
        AgentFlowProperties properties = new AgentFlowProperties(true, timeout, shutdownTimeout,
                Map.of("flow", new AgentFlowConfigDTO("chatModel", "model-name", "planner",
                        List.of(new AgentConfigDTO("planner", "planner", "plan", null)), List.of())));
        return new DefaultAgentFlowService(registry, new AgentFlowSessionExecutionGuard(), properties,
                new ValidationUtils(validatorFactory.getValidator()),
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    }

    private static AgentFlowSessionCommand createCommand() {
        return new AgentFlowSessionCommand("flow", "user", null);
    }

    private static AgentFlowExecutionCommand command(String sessionId) {
        return new AgentFlowExecutionCommand("flow", "user", sessionId, Content.fromParts(Part.fromText("question")));
    }

    private static Event event(String id) {
        return Event.builder().id(id).author("planner").content(Content.fromParts(Part.fromText(id))).build();
    }

    private static final class ControlledRunner extends InMemoryRunner {
        private final Supplier<Flowable<Event>> scripted;

        private ControlledRunner(Flowable<Event> scripted) {
            super(new TestAgent("planner"), "flow");
            this.scripted = () -> scripted;
        }

        private ControlledRunner(Supplier<Flowable<Event>> scripted) {
            super(new TestAgent("planner"), "flow");
            this.scripted = scripted;
        }

        @Override
        public Flowable<Event> runAsync(String userId, String sessionId, Content newMessage) {
            return scripted.get();
        }
    }

    private static final class TestAgent extends BaseAgent {
        private TestAgent(String name) {
            super(name, name, List.of(), List.of(), List.of());
        }

        @Override
        protected Flowable<Event> runAsyncImpl(InvocationContext invocationContext) {
            return Flowable.empty();
        }

        @Override
        protected Flowable<Event> runLiveImpl(InvocationContext invocationContext) {
            return Flowable.empty();
        }
    }
}
