package top.egon.cola.component.bytecode.agent;

import top.egon.cola.component.bytecode.bridge.BridgeCapability;

import java.util.List;
import java.util.Set;

public record AgentConfiguration(
        boolean enabled,
        Set<BridgeCapability> features,
        List<String> includes,
        List<String> excludes,
        AgentFailurePolicy failurePolicy,
        int failureCapacity,
        String includeDigest,
        String excludeDigest
) {
    public AgentConfiguration {
        features = Set.copyOf(features);
        includes = List.copyOf(includes);
        excludes = List.copyOf(excludes);
        if (failureCapacity < 1 || failureCapacity > 1024) {
            throw new IllegalArgumentException("failureCapacity must be between 1 and 1024");
        }
        if (enabled && includes.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one explicit include pattern is required when the Agent is enabled");
        }
    }
}
