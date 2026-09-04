package top.egon.cola.component.agentflow.runtime;

import io.reactivex.rxjava3.core.Completable;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.agentflow.api.AgentFlowDescriptorDTO;
import top.egon.cola.component.agentflow.exception.AgentFlowException;
import top.egon.cola.component.agentflow.exception.AgentFlowNotFoundException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Immutable flow registry that owns runner shutdown. */
@Slf4j
public class DefaultAgentFlowRegistry implements AgentFlowRegistry {

    private enum State {
        OPEN,
        CLOSING,
        CLOSED
    }

    private final Map<String, AgentFlowRuntimeBO> runtimes;
    private final List<AgentFlowDescriptorDTO> descriptors;
    private final Duration shutdownTimeout;
    private final AtomicReference<State> state = new AtomicReference<>(State.OPEN);
    private final AtomicReference<Completable> closeOperation = new AtomicReference<>();

    public DefaultAgentFlowRegistry(Map<String, AgentFlowRuntimeBO> runtimes, Duration shutdownTimeout) {
        if (runtimes == null || shutdownTimeout == null || shutdownTimeout.isNegative() || shutdownTimeout.isZero()) {
            throw new IllegalArgumentException("registry values must be present and positive");
        }
        this.runtimes = Map.copyOf(runtimes);
        this.descriptors = this.runtimes.values().stream()
                .sorted(Comparator.comparing(AgentFlowRuntimeBO::flowId))
                .map(AgentFlowRuntimeBO::descriptor)
                .toList();
        this.shutdownTimeout = shutdownTimeout;
    }

    @Override
    public List<AgentFlowDescriptorDTO> listFlows() {
        requireOpen();
        return descriptors;
    }

    @Override
    public AgentFlowRuntimeBO require(String flowId) {
        requireOpen();
        AgentFlowRuntimeBO runtime = runtimes.get(flowId);
        if (runtime == null) {
            throw new AgentFlowNotFoundException(flowId);
        }
        return runtime;
    }

    @Override
    public boolean beginClosing() {
        return state.compareAndSet(State.OPEN, State.CLOSING);
    }

    @Override
    public synchronized Completable close() {
        Completable existing = closeOperation.get();
        if (existing != null) {
            return existing;
        }
        state.set(State.CLOSING);
        List<Completable> closes = new ArrayList<>(runtimes.values()).stream()
                .sorted(Comparator.comparing(AgentFlowRuntimeBO::flowId))
                .map(AgentFlowRuntimeBO::runner)
                .map(com.google.adk.runner.InMemoryRunner::close)
                .toList();
        Completable close = Completable.mergeDelayError(closes)
                .timeout(shutdownTimeout.toNanos(), TimeUnit.NANOSECONDS)
                .doOnComplete(() -> log.debug("Closed Agent Flow registry stage=SHUTDOWN outcome=SUCCESS"))
                .doOnError(error -> log.warn("Closed Agent Flow registry stage=SHUTDOWN outcome=ERROR errorType={}",
                        error.getClass().getSimpleName()))
                .doFinally(() -> state.set(State.CLOSED))
                .cache();
        closeOperation.set(close);
        return close;
    }

    @Override
    public boolean isClosed() {
        return state.get() == State.CLOSED;
    }

    private void requireOpen() {
        State current = state.get();
        if (current != State.OPEN) {
            throw new AgentFlowException("Agent Flow registry is not open: " + current);
        }
    }
}
