package top.egon.cola.component.gateway.admin.application.scope;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.ddc.error.management.DdcManagementClientException;
import top.egon.cola.component.ddc.model.management.DdcManagementScopeBinding;
import top.egon.cola.component.ddc.model.management.DdcManagementScopeQuery;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayApplicationEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayApplicationRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class GatewayScopeService {

    private static final Comparator<DdcManagementScopeBinding> BINDING_ORDER =
            Comparator.comparing(DdcManagementScopeBinding::bizCode)
                    .thenComparing(DdcManagementScopeBinding::namespaceCode)
                    .thenComparing(DdcManagementScopeBinding::env)
                    .thenComparing(DdcManagementScopeBinding::appCode);

    private final DdcManagementClient client;

    private final GatewayApplicationRepository applications;

    @Autowired
    public GatewayScopeService(
            ObjectProvider<DdcManagementClient> client,
            GatewayApplicationRepository applications) {
        this(client.getIfAvailable(), applications);
    }

    GatewayScopeService(
            DdcManagementClient client,
            GatewayApplicationRepository applications) {
        this.client = client;
        this.applications = applications;
    }

    public List<ScopeView> list() {
        Map<PhysicalApplicationKey, String> connected = applications
                .findAllByDeletedFalseOrderByCreatedAtDesc().stream()
                .collect(Collectors.toMap(
                        GatewayScopeService::physicalKey,
                        GatewayApplicationEntity::getId,
                        (existing, duplicate) -> existing,
                        LinkedHashMap::new
                ));
        return bindings(new ScopeQuery(null, null, null, null)).stream()
                .map(binding -> view(binding, connected))
                .toList();
    }

    public List<DdcManagementScopeBinding> bindings(ScopeQuery query) {
        Objects.requireNonNull(query, "query");
        try {
            return client().getScopeBindings(new DdcManagementScopeQuery(
                            query.bizCode(),
                            query.namespace(),
                            query.env(),
                            query.appCode()
                    )).stream()
                    .filter(DdcManagementScopeBinding::enabled)
                    .sorted(BINDING_ORDER)
                    .toList();
        } catch (DdcManagementClientException
                 | UnsupportedOperationException error) {
            throw new IllegalStateException(
                    "DDC scope catalog is unavailable",
                    error
            );
        }
    }

    public DdcManagementScopeBinding requireEnabled(ScopeQuery query) {
        return bindings(query).stream()
                .filter(value -> exact(value, query))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "DDC scope binding is not enabled"
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

    private static ScopeView view(
            DdcManagementScopeBinding binding,
            Map<PhysicalApplicationKey, String> connected) {
        String applicationId = connected.get(physicalKey(binding));
        return new ScopeView(
                binding.bindingId(),
                binding.bizCode(),
                binding.namespaceCode(),
                binding.env(),
                binding.appCode(),
                binding.appName(),
                applicationId != null,
                applicationId
        );
    }

    private static boolean exact(
            DdcManagementScopeBinding binding,
            ScopeQuery query) {
        return Objects.equals(binding.bizCode(), query.bizCode())
                && Objects.equals(binding.namespaceCode(), query.namespace())
                && Objects.equals(binding.env(), query.env())
                && Objects.equals(binding.appCode(), query.appCode());
    }

    private static PhysicalApplicationKey physicalKey(
            GatewayApplicationEntity application) {
        return new PhysicalApplicationKey(
                application.getBizCode(),
                application.getEnv(),
                application.getApplicationCode()
        );
    }

    private static PhysicalApplicationKey physicalKey(
            DdcManagementScopeBinding binding) {
        return new PhysicalApplicationKey(
                binding.bizCode(),
                binding.env(),
                binding.appCode()
        );
    }

    public record ScopeQuery(
            String bizCode,
            String namespace,
            String env,
            String appCode
    ) {
        public boolean empty() {
            return Stream.of(bizCode, namespace, env, appCode)
                    .allMatch(value -> value == null || value.isBlank());
        }
    }

    public record PhysicalApplicationKey(
            String bizCode,
            String env,
            String appCode
    ) {
    }

    public record ScopeView(
            String bindingId,
            String bizCode,
            String namespace,
            String env,
            String appCode,
            String appName,
            boolean connected,
            String gatewayApplicationId
    ) {
    }
}
