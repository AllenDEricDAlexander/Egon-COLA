package top.egon.cola.component.bytecode.agent;

import top.egon.cola.component.bytecode.bridge.BridgeCapability;

import java.util.List;
import java.util.Set;

public record AgentConfiguration(
        boolean enabled,
        Set<BridgeCapability> features,
        List<String> includes,
        List<String> excludes,
        List<String> observationIncludes,
        List<String> observationMethods,
        List<String> observationExcludes,
        boolean observeConstructors,
        long observationSlowThresholdMillis,
        AgentFailurePolicy failurePolicy,
        int failureCapacity,
        String includeDigest,
        String excludeDigest
) {
    public AgentConfiguration {
        features = Set.copyOf(features);
        includes = List.copyOf(includes);
        excludes = List.copyOf(excludes);
        observationIncludes = List.copyOf(observationIncludes);
        observationMethods = List.copyOf(observationMethods);
        observationExcludes = List.copyOf(observationExcludes);
        if (failureCapacity < 1 || failureCapacity > 1024) {
            throw new IllegalArgumentException("failureCapacity must be between 1 and 1024");
        }
        if (enabled && includes.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one explicit include pattern is required when the Agent is enabled");
        }
        if (observationSlowThresholdMillis < -1L) {
            throw new IllegalArgumentException(
                    "observationSlowThresholdMillis must be -1 or non-negative");
        }
    }

    public boolean methodExtensionEnabled() {
        return enabled && features.contains(BridgeCapability.METHOD_EXTENSION);
    }

    public boolean accessGuardEnabled() {
        return enabled && features.contains(BridgeCapability.ACCESS_GUARD);
    }
}
