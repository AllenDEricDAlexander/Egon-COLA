package top.egon.cola.platform.rbac3.contract.activation;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record RoleActivationCandidateView(
        List<ApplicationCandidates> applications,
        long basedOnAuthVersion,
        long basedOnPolicyVersion,
        String basedOnDirectorySnapshotVersion,
        List<ConfigurationError> configurationErrors,
        Instant calculatedAt
) {

    public RoleActivationCandidateView {
        applications = List.copyOf(Objects.requireNonNull(
                applications,
                "applications"
        ));
        nonNegative(basedOnAuthVersion, "basedOnAuthVersion");
        nonNegative(basedOnPolicyVersion, "basedOnPolicyVersion");
        basedOnDirectorySnapshotVersion = required(
                basedOnDirectorySnapshotVersion,
                "basedOnDirectorySnapshotVersion"
        );
        configurationErrors = List.copyOf(Objects.requireNonNull(
                configurationErrors,
                "configurationErrors"
        ));
        calculatedAt = Objects.requireNonNull(calculatedAt, "calculatedAt");
    }

    public record ApplicationCandidates(
            String applicationId,
            String applicationCode,
            List<RoleActivationCandidate> candidates
    ) {

        public ApplicationCandidates {
            applicationId = required(applicationId, "applicationId");
            applicationCode = required(
                    applicationCode,
                    "applicationCode"
            );
            candidates = List.copyOf(Objects.requireNonNull(
                    candidates,
                    "candidates"
            ));
        }
    }

    public record ConfigurationError(
            String reasonCode,
            List<String> evidenceIds
    ) {

        public ConfigurationError {
            reasonCode = required(reasonCode, "reasonCode");
            evidenceIds = List.copyOf(Objects.requireNonNull(
                    evidenceIds,
                    "evidenceIds"
            ));
            evidenceIds.forEach(id -> required(id, "evidenceIds"));
        }
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static void nonNegative(long value, String fieldName) {
        if (value < 0L) {
            throw new IllegalArgumentException(
                    fieldName + " must not be negative"
            );
        }
    }
}
