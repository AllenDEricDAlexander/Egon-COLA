package top.egon.cola.component.accessguard.core.plan;

import java.util.Objects;

public record GuardPlan(
        String id,
        boolean enabled,
        KeyConfig key,
        AdmissionConfig admission,
        ExecutionConfig execution,
        FailurePolicies failurePolicies,
        ObservabilityConfig observability,
        String stateVersion
) {

    public GuardPlan {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        id = id.trim();
        key = Objects.requireNonNull(key, "key");
        admission = Objects.requireNonNull(admission, "admission");
        execution = Objects.requireNonNull(execution, "execution");
        failurePolicies = Objects.requireNonNull(failurePolicies, "failurePolicies");
        observability = Objects.requireNonNull(observability, "observability");
        if (stateVersion == null || stateVersion.isBlank()) {
            throw new IllegalArgumentException("stateVersion must not be blank");
        }
        stateVersion = stateVersion.trim();
    }
}
