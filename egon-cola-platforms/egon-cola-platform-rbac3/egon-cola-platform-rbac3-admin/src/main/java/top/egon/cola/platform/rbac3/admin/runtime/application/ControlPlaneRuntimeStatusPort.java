package top.egon.cola.platform.rbac3.admin.runtime.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Read-only boundary for Gateway definition, DDC lease and release observations.
 */
@FunctionalInterface
public interface ControlPlaneRuntimeStatusPort {

    RuntimeStatus status();

    record RuntimeStatus(
            DdcConfigClientStatus ddcConfigClient,
            DefinitionStatus definition,
            ProviderLeaseStatus providerLease,
            GatewayReleaseStatus gatewayRelease,
            FlywayStatus flyway,
            RedisProjectionStatus redisProjection,
            FenceMutationStatus fence,
            OutboxStatus outbox,
            Instant checkedAt) {

        public RuntimeStatus {
            ddcConfigClient = ddcConfigClient == null
                    ? DdcConfigClientStatus.unknown()
                    : ddcConfigClient;
        }

        public RuntimeStatus(
                DefinitionStatus definition,
                ProviderLeaseStatus providerLease,
                GatewayReleaseStatus gatewayRelease,
                Instant checkedAt) {
            this(DdcConfigClientStatus.unknown(), definition, providerLease,
                    gatewayRelease, checkedAt);
        }

        public RuntimeStatus(
                DdcConfigClientStatus ddcConfigClient,
                DefinitionStatus definition,
                ProviderLeaseStatus providerLease,
                GatewayReleaseStatus gatewayRelease,
                Instant checkedAt) {
            this(ddcConfigClient, definition, providerLease, gatewayRelease,
                    new FlywayStatus("UNKNOWN", "UNKNOWN"),
                    new RedisProjectionStatus("UNKNOWN", 0L),
                    new FenceMutationStatus("UNKNOWN", 0L, 0L, 0L),
                    new OutboxStatus("UNKNOWN", 0L, 0L),
                    checkedAt);
        }
    }

    record DdcConfigClientStatus(
            String state,
            String instanceId,
            String leaseIdFingerprint,
            Instant leaseExpireAt,
            Map<String, Long> configVersions,
            String lastApplyFailureKey,
            Long lastApplyFailureVersion,
            String lastApplyFailureCode) {

        public DdcConfigClientStatus {
            configVersions = configVersions == null ? Map.of() : Map.copyOf(configVersions);
        }

        public static DdcConfigClientStatus unknown() {
            return new DdcConfigClientStatus(
                    "UNKNOWN", null, null, null, Map.of(), null, null, null);
        }
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

    record FlywayStatus(String rbac3History, String outboxHistory) {
    }

    record RedisProjectionStatus(String state, long checkpointLag) {
    }

    record FenceMutationStatus(
            String state,
            long pendingCount,
            long recoveryRequiredCount,
            long oldestAgeSeconds) {
    }

    record OutboxStatus(
            String state,
            long pendingCount,
            long oldestAgeSeconds) {
    }
}
