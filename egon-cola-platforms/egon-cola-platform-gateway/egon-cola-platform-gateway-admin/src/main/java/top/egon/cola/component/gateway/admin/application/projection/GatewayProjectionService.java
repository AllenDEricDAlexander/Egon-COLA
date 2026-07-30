package top.egon.cola.component.gateway.admin.application.projection;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.egon.cola.component.ddc.management.DdcManagementClient;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigClientInstance;
import top.egon.cola.component.ddc.management.model.DdcInstanceStatus;
import top.egon.cola.component.ddc.management.model.DdcManagementInstanceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceInstance;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceKey;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceSnapshot;
import top.egon.cola.component.gateway.admin.application.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.application.release.GatewayReleaseService;
import top.egon.cola.component.gateway.admin.application.release.GatewayReleaseStore;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayGroupEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayGroupRepository;
import top.egon.cola.component.gateway.admin.rule.GatewayDdcRulePublisher;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class GatewayProjectionService {

    private final GatewayGroupRepository groups;

    private final GatewayReleaseService releases;

    private final DdcManagementClient client;

    private final Map<String, ProjectionEnvelope<?>> cache =
            new ConcurrentHashMap<>();

    private final Clock clock;

    @Autowired
    public GatewayProjectionService(
            GatewayGroupRepository groups,
            GatewayReleaseService releases,
            ObjectProvider<DdcManagementClient> client) {
        this(
                groups,
                releases,
                client.getIfAvailable(),
                Clock.systemUTC()
        );
    }

    GatewayProjectionService(
            GatewayGroupRepository groups,
            GatewayReleaseService releases,
            DdcManagementClient client,
            Clock clock) {
        this.groups = groups;
        this.releases = releases;
        this.client = client;
        this.clock = clock;
    }

    public ProjectionEnvelope<List<DdcManagementConfigClientInstance>>
    engineNodes(String gatewayGroupId) {
        GatewayGroupEntity group = group(gatewayGroupId);
        String key = "engine:" + gatewayGroupId;
        return load(key, "DDC_CONFIG_CLIENT", () -> client()
                .getConfigClients(new DdcManagementInstanceQuery(
                        GatewayDdcRulePublisher.appCode(
                                group.getGatewayGroupCode()
                        ),
                        group.getEnv(),
                        group.getNamespace()
                )));
    }

    public ProjectionEnvelope<DdcManagementServiceCatalog> services(
            ProviderQuery query) {
        String key = "services:" + query;
        return load(key, "DDC_SERVICE_REGISTRY", () -> client()
                .getServiceKeys(query.ddc()));
    }

    public ProjectionEnvelope<DdcManagementServiceSnapshot> instances(
            ProviderQuery query) {
        String key = "instances:" + query;
        return load(key, "DDC_SERVICE_REGISTRY", () -> client()
                .getInstances(query.ddc()));
    }

    public ProjectionEnvelope<List<ProviderInstanceProjection>> instances(
            String env,
            String namespace) {
        String key = "instances:" + env + ":" + namespace;
        return load(key, "DDC_SERVICE_REGISTRY", () -> {
            List<ProviderInstanceProjection> result = new ArrayList<>();
            collectInstances(
                    result,
                    env,
                    namespace,
                    "HTTP_PROVIDER",
                    "http"
            );
            collectInstances(
                    result,
                    env,
                    namespace,
                    "HTTP_PROVIDER",
                    "https"
            );
            collectInstances(
                    result,
                    env,
                    namespace,
                    "RPC_PROVIDER",
                    "grpc"
            );
            return List.copyOf(result);
        });
    }

    public RuntimeConsistency runtimeConsistency(String gatewayGroupId) {
        List<GatewayReleaseService.ReleaseView> history =
                releases.history(gatewayGroupId);
        GatewayReleaseService.ReleaseView target = history.isEmpty()
                ? null
                : history.getFirst();
        ProjectionEnvelope<List<DdcManagementConfigClientInstance>> nodes =
                engineNodes(gatewayGroupId);
        GatewayReleaseStore.AttemptRecord attempt = latestSuccessfulAttempt(
                target
        );
        RuleExpectation expectation = expectation(attempt);
        List<EngineNodeConsistency> nodeStates = nodes.value().stream()
                .map(node -> nodeConsistency(
                        node,
                        target,
                        expectation
                ))
                .toList();
        long ready = nodeStates.stream()
                .filter(node -> "CONSISTENT".equals(node.status()))
                .count();
        return new RuntimeConsistency(
                target == null ? null : target.releaseId(),
                target == null ? null : target.status().name(),
                nodes.value().size(),
                ready,
                target != null
                        && "SUCCESS".equals(target.status().name())
                        && !nodes.value().isEmpty()
                        && ready == nodes.value().size(),
                nodes.observedAt(),
                nodes.source(),
                nodes.stale(),
                nodeStates
        );
    }

    private GatewayReleaseStore.AttemptRecord latestSuccessfulAttempt(
            GatewayReleaseService.ReleaseView release) {
        if (release == null) {
            return null;
        }
        return release.attempts().stream()
                .filter(attempt -> "SUCCESS".equals(attempt.status()))
                .max(java.util.Comparator.comparingInt(
                        GatewayReleaseStore.AttemptRecord::attemptNo
                ))
                .orElse(null);
    }

    private EngineNodeConsistency nodeConsistency(
            DdcManagementConfigClientInstance node,
            GatewayReleaseService.ReleaseView release,
            RuleExpectation expectation) {
        if (!online(node)) {
            return nodeState(node, "NOT_READY", "NODE_OFFLINE");
        }
        if (release == null || release.status() !=
                top.egon.cola.component.gateway.admin.domain
                        .GatewayReleaseStatus.SUCCESS) {
            return nodeState(node, "INCONSISTENT", "RELEASE_NOT_READY");
        }
        Map<String, String> metadata = node.metadata();
        if (!release.releaseId().equals(metadata.get("activeReleaseId"))) {
            return nodeState(node, "INCONSISTENT", "RELEASE_MISMATCH");
        }
        if (expectation == null) {
            return nodeState(node, "INCONSISTENT", "ACK_MISSING");
        }
        if (!Objects.equals(
                value(expectation.version()),
                metadata.get("activeRuleVersion")
        )) {
            return nodeState(node, "INCONSISTENT", "VERSION_MISMATCH");
        }
        if (!Objects.equals(
                expectation.artifactSha256(),
                metadata.get("activeRuleChecksum")
        )) {
            return nodeState(node, "INCONSISTENT", "CHECKSUM_MISMATCH");
        }
        if (!"ACK_SUCCESS".equals(metadata.get("lastApplyStatus"))
                || instantValue(metadata.get("lastAckAt")) == null) {
            return nodeState(node, "INCONSISTENT", "APPLY_NOT_ACKED");
        }
        return nodeState(node, "CONSISTENT", null);
    }

    private RuleExpectation expectation(
            GatewayReleaseStore.AttemptRecord attempt) {
        if (attempt == null) {
            return null;
        }
        List<GatewayReleaseStore.TargetRecord> targets = attempt.targets();
        if (targets.isEmpty() || targets.stream().anyMatch(target ->
                !"SUCCESS".equals(target.status())
                        || target.appliedVersion() == null
                        || target.appliedArtifactSha256() == null
                        || target.appliedArtifactSha256().isBlank())) {
            return null;
        }
        GatewayReleaseStore.TargetRecord first = targets.getFirst();
        boolean unanimous = targets.stream().allMatch(target ->
                Objects.equals(
                        first.appliedVersion(),
                        target.appliedVersion()
                )
                        && Objects.equals(
                        first.appliedArtifactSha256(),
                        target.appliedArtifactSha256()
                ));
        return unanimous
                ? new RuleExpectation(
                first.appliedVersion(),
                first.appliedArtifactSha256()
        )
                : null;
    }

    private EngineNodeConsistency nodeState(
            DdcManagementConfigClientInstance node,
            String status,
            String reason) {
        Map<String, String> metadata = node.metadata();
        return new EngineNodeConsistency(
                node.instanceId(),
                node.leaseId(),
                node.status(),
                status,
                reason,
                metadata.get("activeReleaseId"),
                longValue(metadata.get("activeRuleVersion")),
                metadata.get("activeRuleChecksum"),
                metadata.get("lastApplyStatus"),
                instantValue(metadata.get("lastAckAt"))
        );
    }

    private String value(Long value) {
        return value == null ? null : value.toString();
    }

    private Long longValue(String value) {
        try {
            return value == null || value.isBlank()
                    ? null
                    : Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Instant instantValue(String value) {
        try {
            return value == null || value.isBlank()
                    ? null
                    : Instant.parse(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public ProjectionCounts scopeCounts(String env, String namespace) {
        long totalEngines = 0;
        long readyEngines = 0;
        long inconsistentGroups = 0;
        boolean stale = false;
        List<GatewayGroupEntity> scopedGroups = groups
                .findAllByEnvAndNamespaceAndDeletedFalseOrderByCreatedAtDesc(
                        env,
                        namespace
                );
        for (GatewayGroupEntity group : scopedGroups) {
            if (!group.isEnabled()) {
                continue;
            }
            RuntimeConsistency consistency =
                    runtimeConsistency(group.getId());
            totalEngines += consistency.engineNodeCount();
            readyEngines += consistency.readyEngineNodeCount();
            inconsistentGroups += consistency.consistent() ? 0 : 1;
            stale = stale || consistency.stale();
        }
        ProjectionEnvelope<List<ProviderInstanceProjection>> providers =
                instances(env, namespace);
        long activeProviders = providers.value().stream()
                .filter(this::online)
                .count();
        return new ProjectionCounts(
                readyEngines,
                totalEngines,
                inconsistentGroups,
                activeProviders,
                providers.value().size() - activeProviders,
                stale || providers.stale()
        );
    }

    private GatewayGroupEntity group(String id) {
        return groups.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "gateway group " + id + " was not found"
                ));
    }

    private DdcManagementClient client() {
        if (client == null) {
            throw new IllegalStateException(
                    "DDC management client is not configured"
            );
        }
        return client;
    }

    private void collectInstances(
            List<ProviderInstanceProjection> result,
            String env,
            String namespace,
            String serviceKind,
            String protocol) {
        DdcManagementServiceCatalog catalog = client().getServiceKeys(
                new DdcManagementServiceQuery(
                        env,
                        namespace,
                        serviceKind,
                        protocol,
                        null,
                        null,
                        null
                )
        );
        for (DdcManagementServiceKey service : catalog.services()) {
            DdcManagementServiceSnapshot snapshot = client().getInstances(
                    new DdcManagementServiceQuery(
                            service.env(),
                            service.namespace(),
                            service.serviceKind(),
                            service.protocol(),
                            service.serviceName(),
                            service.group(),
                            service.version()
                    )
            );
            snapshot.instances().forEach(instance -> result.add(
                    projection(service, instance, snapshot.observedAt())
            ));
        }
    }

    private ProviderInstanceProjection projection(
            DdcManagementServiceKey service,
            DdcManagementServiceInstance instance,
            Instant observedAt) {
        Map<String, String> metadata = instance.metadata();
        return new ProviderInstanceProjection(
                String.join(
                        ":",
                        service.serviceKind(),
                        service.protocol(),
                        service.serviceName(),
                        value(service.group()),
                        value(service.version())
                ),
                service.protocol(),
                service.serviceName(),
                service.group(),
                service.version(),
                instance.instanceId(),
                instance.leaseId(),
                instance.host(),
                instance.port(),
                metadata.get("gateway.region"),
                metadata.get("gateway.zone"),
                integer(metadata.get("gateway.weight")),
                metadata,
                definitionSetId(metadata),
                instance.normalizedStatus().name(),
                instance.expireAt(),
                observedAt
        );
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private String definitionSetId(Map<String, String> metadata) {
        String canonical = metadata.get("gateway.definition-set-id");
        return canonical == null || canonical.isBlank()
                ? metadata.get("gateway.definition-set")
                : canonical;
    }

    private Integer integer(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean online(DdcManagementConfigClientInstance instance) {
        return instance.normalizedStatus().isAvailable(
                clock.instant(),
                instance.expireAt()
        );
    }

    private boolean online(ProviderInstanceProjection instance) {
        return DdcInstanceStatus.fromWire(instance.status()).isAvailable(
                clock.instant(),
                instance.expireAt()
        );
    }

    @SuppressWarnings("unchecked")
    private <T> ProjectionEnvelope<T> load(
            String key,
            String source,
            Supplier<T> loader) {
        try {
            ProjectionEnvelope<T> value = new ProjectionEnvelope<>(
                    loader.get(),
                    clock.instant(),
                    source,
                    false,
                    null
            );
            cache.put(key, value);
            return value;
        } catch (RuntimeException failure) {
            ProjectionEnvelope<T> previous =
                    (ProjectionEnvelope<T>) cache.get(key);
            if (previous != null) {
                return new ProjectionEnvelope<>(
                        previous.value(),
                        previous.observedAt(),
                        previous.source(),
                        true,
                        bounded(failure.getMessage())
                );
            }
            throw failure;
        }
    }

    private String bounded(String value) {
        if (value == null || value.isBlank()) {
            return "projection refresh failed";
        }
        return value.length() <= 512 ? value : value.substring(0, 512);
    }

    public record ProjectionEnvelope<T>(
            T value,
            Instant observedAt,
            String source,
            boolean stale,
            String refreshError
    ) {
    }

    public record ProviderQuery(
            String env,
            String namespace,
            String serviceKind,
            String protocol,
            String serviceName,
            String group,
            String version
    ) {

        private DdcManagementServiceQuery ddc() {
            String ddcProtocol = protocol.trim().toLowerCase(Locale.ROOT);
            if ("rpc".equals(ddcProtocol)) {
                ddcProtocol = "grpc";
            }
            String ddcServiceKind = serviceKind;
            if (ddcServiceKind == null || ddcServiceKind.isBlank()) {
                ddcServiceKind = switch (ddcProtocol) {
                    case "http", "https" -> "HTTP_PROVIDER";
                    case "grpc" -> "RPC_PROVIDER";
                    default -> throw new IllegalArgumentException(
                            "serviceKind is required for protocol "
                                    + protocol
                    );
                };
            } else {
                ddcServiceKind = ddcServiceKind.trim()
                        .toUpperCase(Locale.ROOT);
            }
            return new DdcManagementServiceQuery(
                    env,
                    namespace,
                    ddcServiceKind,
                    ddcProtocol,
                    serviceName,
                    group,
                    version
            );
        }
    }

    public record ProviderInstanceProjection(
            String serviceKey,
            String protocol,
            String serviceName,
            String group,
            String version,
            String instanceId,
            String leaseId,
            String host,
            int port,
            String region,
            String zone,
            Integer weight,
            Map<String, String> tags,
            String definitionSetId,
            String status,
            Instant expireAt,
            Instant observedAt
    ) {

        public ProviderInstanceProjection {
            tags = Map.copyOf(tags);
        }
    }

    public record RuntimeConsistency(
            String targetReleaseId,
            String targetReleaseStatus,
            int engineNodeCount,
            long readyEngineNodeCount,
            boolean consistent,
            Instant observedAt,
            String source,
            boolean stale,
            List<EngineNodeConsistency> nodes
    ) {

        public RuntimeConsistency {
            nodes = List.copyOf(nodes);
        }
    }

    public record EngineNodeConsistency(
            String instanceId,
            String leaseId,
            String leaseStatus,
            String status,
            String reason,
            String activeReleaseId,
            Long activeRuleVersion,
            String activeRuleChecksum,
            String lastApplyStatus,
            Instant lastAckAt
    ) {
    }

    private record RuleExpectation(
            Long version,
            String artifactSha256
    ) {
    }

    public record ProjectionCounts(
            long readyEngines,
            long totalEngines,
            long inconsistentGroups,
            long activeProviders,
            long abnormalProviders,
            boolean stale
    ) {
    }
}
