package top.egon.cola.platform.rbac3.admin.integration.runtime;

import top.egon.cola.platform.rbac3.admin.integration.ddc.DdcProviderLeaseStatusService;
import top.egon.cola.platform.rbac3.admin.integration.gateway.GatewayAdminControlPlaneStatusClient;
import top.egon.cola.platform.rbac3.admin.integration.gateway.GatewayDefinitionStatusService;
import top.egon.cola.platform.rbac3.admin.runtime.application.ControlPlaneRuntimeStatusPort;

import java.time.Clock;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Aggregates, but never collapses, definition, provider lease and release state.
 */
public final class GatewayDdcRuntimeStatusService
        implements ControlPlaneRuntimeStatusPort {

    private final Supplier<GatewayDefinitionStatusService.DefinitionStatus> definition;
    private final Supplier<DdcProviderLeaseStatusService.ProviderLeaseStatus> lease;
    private final GatewayAdminControlPlaneStatusClient gatewayAdmin;
    private final ServiceIdentity expectedIdentity;
    private final Clock clock;

    public GatewayDdcRuntimeStatusService(
            GatewayDefinitionStatusService definition,
            DdcProviderLeaseStatusService lease,
            GatewayAdminControlPlaneStatusClient gatewayAdmin,
            ServiceIdentity expectedIdentity,
            Clock clock) {
        this(definition::status, lease::status, gatewayAdmin, expectedIdentity, clock);
    }

    public GatewayDdcRuntimeStatusService(
            Supplier<GatewayDefinitionStatusService.DefinitionStatus> definition,
            Supplier<DdcProviderLeaseStatusService.ProviderLeaseStatus> lease,
            GatewayAdminControlPlaneStatusClient gatewayAdmin,
            ServiceIdentity expectedIdentity,
            Clock clock) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.lease = Objects.requireNonNull(lease, "lease");
        this.gatewayAdmin = Objects.requireNonNull(gatewayAdmin, "gatewayAdmin");
        this.expectedIdentity = Objects.requireNonNull(expectedIdentity, "expectedIdentity");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public RuntimeStatus status() {
        var definitionStatus = definition.get();
        var leaseStatus = lease.get();
        var gateway = gatewayAdmin.snapshot();
        String routeability = routeability(definitionStatus, leaseStatus, gateway);
        return new RuntimeStatus(
                new DefinitionStatus(
                        definitionStatus.status(), definitionStatus.definitionSetId(),
                        definitionStatus.warnings()),
                new ProviderLeaseStatus(
                        leaseStatus.state(), leaseStatus.instanceId(),
                        leaseStatus.leaseExpireAt()),
                new GatewayReleaseStatus(
                        gateway.release().releaseId(), routeability,
                        gateway.consistency().observedVersion()),
                clock.instant());
    }

    private String routeability(
            GatewayDefinitionStatusService.DefinitionStatus definitionStatus,
            DdcProviderLeaseStatusService.ProviderLeaseStatus leaseStatus,
            GatewayAdminControlPlaneStatusClient.GatewayAdminSnapshot gateway) {
        if (unknown(gateway)) {
            return "UNKNOWN";
        }
        if (!definitionStatus.accepted()
                || !"REGISTERED".equals(leaseStatus.state())
                || !expectedIdentity.equals(definitionStatus.identity())
                || !expectedIdentity.equals(leaseStatus.identity())
                || !"SUCCESS".equals(gateway.release().releaseStatus())
                || !Objects.equals(
                        definitionStatus.definitionSetId(),
                        gateway.release().definitionSetId())
                || !Objects.equals(
                        expectedIdentity.version(),
                        gateway.release().publishedVersion())
                || !gateway.consistency().consistent()
                || !Objects.equals(
                        gateway.release().releaseId(), gateway.consistency().releaseId())
                || !"SUCCESS".equals(gateway.consistency().releaseStatus())) {
            return "NOT_ROUTABLE";
        }
        boolean providerMatches = gateway.providers().instances().stream()
                .filter(instance -> "UP".equals(instance.status())
                        || "ONLINE".equals(instance.status())
                        || "ACTIVE".equals(instance.status()))
                .anyMatch(instance -> expectedIdentity.matches(instance.serviceKey())
                        && Objects.equals(
                        definitionStatus.definitionSetId(), instance.definitionSetId()));
        return providerMatches ? "ROUTABLE" : "NOT_ROUTABLE";
    }

    private boolean unknown(
            GatewayAdminControlPlaneStatusClient.GatewayAdminSnapshot gateway) {
        return "UNKNOWN".equals(gateway.release().state())
                || "UNKNOWN".equals(gateway.providers().state())
                || "UNKNOWN".equals(gateway.consistency().state());
    }

    public record ServiceIdentity(
            String bizCode,
            String appCode,
            String env,
            String namespace,
            String serviceKind,
            String protocol,
            String serviceName,
            String group,
            String version) {

        public ServiceIdentity {
            bizCode = required(bizCode, "bizCode");
            appCode = required(appCode, "appCode");
            env = required(env, "env");
            namespace = required(namespace, "namespace");
            serviceKind = required(serviceKind, "serviceKind");
            protocol = required(protocol, "protocol");
            serviceName = required(serviceName, "serviceName");
            group = required(group, "group");
            version = required(version, "version");
        }

        boolean matches(GatewayAdminControlPlaneStatusClient.ServiceKey key) {
            return key != null
                    && bizCode.equals(key.bizCode())
                    && appCode.equals(key.appCode())
                    && env.equals(key.env())
                    && namespace.equals(key.namespace())
                    && serviceKind.equals(key.serviceKind())
                    && protocol.equals(key.protocol())
                    && serviceName.equals(key.serviceName())
                    && group.equals(key.group())
                    && version.equals(key.version());
        }

        private static String required(String value, String field) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field + " is required");
            }
            return value.trim();
        }
    }
}
