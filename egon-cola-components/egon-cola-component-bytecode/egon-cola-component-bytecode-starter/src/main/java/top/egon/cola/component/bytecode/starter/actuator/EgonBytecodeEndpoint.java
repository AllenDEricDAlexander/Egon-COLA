package top.egon.cola.component.bytecode.starter.actuator;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import top.egon.cola.component.bytecode.bridge.AgentBridgeStatus;
import top.egon.cola.component.bytecode.bridge.BridgeStatus;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;
import top.egon.cola.component.bytecode.runtime.observation.ObservationRuntime;

import java.util.LinkedHashMap;
import java.util.Map;

@Endpoint(id = "egonbytecode")
public final class EgonBytecodeEndpoint {

    private final ClassLoader applicationLoader;
    private final ObservationRuntime observationRuntime;

    public EgonBytecodeEndpoint(ClassLoader applicationLoader) {
        this(applicationLoader, null);
    }

    public EgonBytecodeEndpoint(
            ClassLoader applicationLoader,
            ObservationRuntime observationRuntime
    ) {
        this.applicationLoader = applicationLoader;
        this.observationRuntime = observationRuntime;
    }

    @ReadOperation
    public Map<String, Object> status() {
        AgentBridgeStatus agent = DispatcherRegistry.agentStatus();
        BridgeStatus dispatcher = DispatcherRegistry.status(applicationLoader);
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("agentVersion", agent.agentVersion());
        status.put("state", agent.state());
        status.put("protocolMajor", agent.protocolMajor());
        status.put("protocolMinor", agent.protocolMinor());
        status.put("requestedFeatures", agent.requestedFeatures());
        status.put("effectiveFeatures", agent.effectiveFeatures());
        status.put("transformedClassCount", agent.transformedClassCount());
        status.put("skippedClassCount", agent.skippedClassCount());
        status.put("failureCount", agent.failureCount());
        status.put("recentFailures", agent.recentFailures());
        status.put("dispatcherRegistered", dispatcher.registered());
        status.put("runtimeVersion", dispatcher.runtimeVersion());
        status.put("runtimeCapabilities", dispatcher.capabilities());
        status.put("callSiteCount", dispatcher.callSiteCount());
        status.put("methodCount", dispatcher.methodCount());
        if (observationRuntime != null) {
            ObservationRuntime.ObservationSnapshot observation =
                    observationRuntime.snapshot();
            status.put("observationEnteredCount", observation.enteredCount());
            status.put("observationPublishedCount", observation.publishedCount());
            status.put("observationSuccessCount", observation.successCount());
            status.put("observationErrorCount", observation.errorCount());
            status.put("observationSlowCount", observation.slowCount());
            status.put("observationSuppressedCount", observation.suppressedCount());
        }
        return Map.copyOf(status);
    }
}
