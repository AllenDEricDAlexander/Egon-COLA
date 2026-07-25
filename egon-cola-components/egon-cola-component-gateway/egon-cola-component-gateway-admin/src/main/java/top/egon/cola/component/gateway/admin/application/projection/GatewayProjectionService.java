package top.egon.cola.component.gateway.admin.application.projection;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import top.egon.cola.component.ddc.management.DdcManagementClient;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigClientInstance;
import top.egon.cola.component.ddc.management.model.DdcManagementInstanceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceInstance;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceKey;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceSnapshot;
import top.egon.cola.component.gateway.admin.application.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.application.release.GatewayReleaseService;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayGroupEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayGroupRepository;
import top.egon.cola.component.gateway.admin.rule.GatewayDdcRulePublisher;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
                    "HTTP"
            );
            collectInstances(
                    result,
                    env,
                    namespace,
                    "RPC_PROVIDER",
                    "RPC"
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
        long ready = nodes.value().stream()
                .filter(node -> "READY".equals(node.status()))
                .count();
        return new RuntimeConsistency(
                target == null ? null : target.releaseId(),
                target == null ? null : target.status().name(),
                nodes.value().size(),
                ready,
                target != null
                        && "SUCCESS".equals(target.status().name())
                        && ready == nodes.value().size(),
                nodes.observedAt(),
                nodes.source(),
                nodes.stale()
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
                metadata.get("gateway.definition-set"),
                instance.status(),
                instance.expireAt(),
                observedAt
        );
    }

    private String value(String value) {
        return value == null ? "" : value;
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
            return new DdcManagementServiceQuery(
                    env,
                    namespace,
                    serviceKind,
                    protocol,
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
            boolean stale
    ) {
    }
}
