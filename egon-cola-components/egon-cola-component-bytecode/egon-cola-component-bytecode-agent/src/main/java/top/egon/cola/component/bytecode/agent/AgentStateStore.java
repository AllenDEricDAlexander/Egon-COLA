package top.egon.cola.component.bytecode.agent;

import top.egon.cola.component.bytecode.bridge.AgentBridgeFailure;
import top.egon.cola.component.bytecode.bridge.AgentBridgeStatus;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.BridgeProtocol;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class AgentStateStore {

    private static final AgentStateStore GLOBAL = new AgentStateStore(resolveVersion(), 32);

    private final String agentVersion;
    private final AtomicReference<AgentState> state = new AtomicReference<>(AgentState.DISABLED);
    private final AtomicLong transformedClasses = new AtomicLong();
    private final AtomicLong skippedClasses = new AtomicLong();
    private final AtomicLong failureCount = new AtomicLong();
    private final Deque<AgentFailure> failures = new ArrayDeque<>();

    private volatile Set<BridgeCapability> requestedFeatures = Set.of();
    private volatile Set<BridgeCapability> effectiveFeatures = Set.of();
    private volatile int failureCapacity;

    public AgentStateStore(String agentVersion, int failureCapacity) {
        this.agentVersion = agentVersion == null ? "" : agentVersion;
        this.failureCapacity = requireCapacity(failureCapacity);
        publish();
    }

    public static AgentStateStore global() {
        return GLOBAL;
    }

    public void start(AgentConfiguration configuration) {
        transition(AgentState.DISABLED, AgentState.STARTING);
        requestedFeatures = configuration.features();
        effectiveFeatures = configuration.features();
        failureCapacity = requireCapacity(configuration.failureCapacity());
        publish();
    }

    public void active() {
        transition(AgentState.STARTING, AgentState.ACTIVE);
    }

    public void degraded() {
        AgentState current = state.get();
        if (current != AgentState.STARTING && current != AgentState.ACTIVE) {
            throw new IllegalStateException("Cannot transition Agent from " + current + " to DEGRADED");
        }
        state.set(AgentState.DEGRADED);
        publish();
    }

    public void failed() {
        AgentState current = state.get();
        if (current == AgentState.FAILED) {
            publish();
            return;
        }
        if (current != AgentState.STARTING
                && current != AgentState.ACTIVE
                && current != AgentState.DEGRADED) {
            throw new IllegalStateException("Cannot transition Agent from " + current + " to FAILED");
        }
        state.set(AgentState.FAILED);
        effectiveFeatures = Set.of();
        publish();
    }

    public void startupFailed(AgentFailure failure) {
        transition(AgentState.DISABLED, AgentState.STARTING);
        recordFailure(failure);
        failed();
    }

    public AgentState state() {
        return state.get();
    }

    public void transformed() {
        transformedClasses.incrementAndGet();
        publish();
    }

    public void skipped() {
        skippedClasses.incrementAndGet();
        publish();
    }

    public void disableFeature(BridgeCapability capability) {
        if (effectiveFeatures.contains(capability)) {
            java.util.EnumSet<BridgeCapability> remaining = java.util.EnumSet.copyOf(effectiveFeatures);
            remaining.remove(capability);
            effectiveFeatures = remaining.isEmpty() ? Set.of() : Set.copyOf(remaining);
        }
        if (state.get() == AgentState.ACTIVE || state.get() == AgentState.STARTING) {
            degraded();
        } else {
            publish();
        }
    }

    public void recordFailure(AgentFailure failure) {
        failureCount.incrementAndGet();
        synchronized (failures) {
            failures.addLast(failure);
            while (failures.size() > failureCapacity) {
                failures.removeFirst();
            }
        }
        publish();
    }

    public List<AgentFailure> recentFailures() {
        synchronized (failures) {
            return List.copyOf(failures);
        }
    }

    private void transition(AgentState expected, AgentState target) {
        if (!state.compareAndSet(expected, target)) {
            throw new IllegalStateException(
                    "Cannot transition Agent from " + state.get() + " to " + target);
        }
        publish();
    }

    private void publish() {
        List<AgentBridgeFailure> bridgeFailures = new ArrayList<>();
        for (AgentFailure failure : recentFailures()) {
            bridgeFailures.add(new AgentBridgeFailure(
                    failure.timestamp().toString(),
                    failure.className(),
                    failure.classLoader(),
                    failure.feature(),
                    failure.exceptionType(),
                    failure.message(),
                    failure.policy().name()
            ));
        }
        DispatcherRegistry.publishAgentStatus(new AgentBridgeStatus(
                agentVersion,
                state.get().name(),
                BridgeProtocol.MAJOR,
                BridgeProtocol.MINOR,
                requestedFeatures,
                effectiveFeatures,
                transformedClasses.get(),
                skippedClasses.get(),
                failureCount.get(),
                bridgeFailures
        ));
    }

    private int requireCapacity(int capacity) {
        if (capacity < 1 || capacity > 1024) {
            throw new IllegalArgumentException("failure capacity must be between 1 and 1024");
        }
        return capacity;
    }

    private static String resolveVersion() {
        String version = AgentStateStore.class.getPackage().getImplementationVersion();
        return version == null ? "development" : version;
    }
}
