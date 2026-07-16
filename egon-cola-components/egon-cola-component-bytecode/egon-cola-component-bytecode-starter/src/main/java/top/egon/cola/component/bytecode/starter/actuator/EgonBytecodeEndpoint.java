package top.egon.cola.component.bytecode.starter.actuator;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import top.egon.cola.component.bytecode.bridge.AgentBridgeStatus;
import top.egon.cola.component.bytecode.bridge.BridgeStatus;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;

import java.util.LinkedHashMap;
import java.util.Map;

@Endpoint(id = "egonbytecode")
public final class EgonBytecodeEndpoint {

    private final ClassLoader applicationLoader;

    public EgonBytecodeEndpoint(ClassLoader applicationLoader) {
        this.applicationLoader = applicationLoader;
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
        return Map.copyOf(status);
    }
}
