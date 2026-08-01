package top.egon.cola.platform.rbac3.admin.runtime.application;

import java.time.Instant;
import java.util.List;

/**
 * Read-only boundary for Gateway definition, DDC lease and release observations.
 */
@FunctionalInterface
public interface ControlPlaneRuntimeStatusPort {

    RuntimeStatus status();

    record RuntimeStatus(
            DefinitionStatus definition,
            ProviderLeaseStatus providerLease,
            GatewayReleaseStatus gatewayRelease,
            Instant checkedAt) {
    }

    record DefinitionStatus(
            String status,
            String definitionSetId,
            List<String> warnings) {
        public DefinitionStatus {
            warnings = List.copyOf(warnings);
        }
    }

    record ProviderLeaseStatus(
            String state,
            String instanceId,
            Instant leaseExpireAt) {
    }

    record GatewayReleaseStatus(
            String releaseId,
            String status,
            String observedByEngineVersion) {
    }
}
