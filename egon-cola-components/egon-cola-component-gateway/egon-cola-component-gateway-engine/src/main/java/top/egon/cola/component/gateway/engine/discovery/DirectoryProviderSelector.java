package top.egon.cola.component.gateway.engine.discovery;

import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.engine.balance.ProviderLoadBalancer;
import top.egon.cola.component.gateway.engine.balance.ProviderSelectionHandle;
import top.egon.cola.component.gateway.engine.balance.LoadBalancerType;
import top.egon.cola.component.gateway.engine.balance.ProviderLoadBalancers;
import top.egon.cola.component.gateway.engine.http.ProviderSelector;

import java.time.Clock;
import java.util.Objects;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public final class DirectoryProviderSelector implements ProviderSelector {

    private final ProviderDirectory directory;

    private final Map<LoadBalancerType, ProviderLoadBalancer> loadBalancers;

    private final ProviderCandidateFilter candidateFilter;

    private final Function<ProviderServiceKey, ProviderSelectionPolicy> policies;

    private final Supplier<Map<String, RuntimeProviderPolicy>> runtimePolicies;

    public DirectoryProviderSelector(
            ProviderDirectory directory,
            ProviderLoadBalancer loadBalancer) {
        this(
                directory,
                Map.of(LoadBalancerType.ROUND_ROBIN, loadBalancer),
                new ProviderCandidateFilter(Clock.systemUTC(), ignored -> true),
                key -> ProviderSelectionPolicy.defaults(
                        key.transport().equals("https")
                ),
                Map::of
        );
    }

    public DirectoryProviderSelector(
            ProviderDirectory directory,
            Map<LoadBalancerType, ProviderLoadBalancer> loadBalancers,
            ProviderCandidateFilter candidateFilter,
            Function<ProviderServiceKey, ProviderSelectionPolicy> policies,
            Supplier<Map<String, RuntimeProviderPolicy>> runtimePolicies) {
        this.directory = Objects.requireNonNull(directory, "directory");
        EnumMap<LoadBalancerType, ProviderLoadBalancer> copy =
                new EnumMap<>(LoadBalancerType.class);
        copy.putAll(Objects.requireNonNull(loadBalancers, "loadBalancers"));
        this.loadBalancers = Map.copyOf(copy);
        this.candidateFilter = Objects.requireNonNull(
                candidateFilter,
                "candidateFilter"
        );
        this.policies = Objects.requireNonNull(policies, "policies");
        this.runtimePolicies = Objects.requireNonNull(
                runtimePolicies,
                "runtimePolicies"
        );
    }

    @Override
    public ProviderSelectionHandle select(ProviderServiceKey serviceKey) {
        return select(serviceKey, Set.of());
    }

    @Override
    public ProviderSelectionHandle select(
            ProviderServiceKey serviceKey,
            Set<String> policyRefs) {
        ResolvedPolicies resolved = resolve(serviceKey, policyRefs);
        ProviderCandidateFilterResult result = candidateFilter.filter(
                serviceKey,
                directory.instances(serviceKey),
                resolved.selectionPolicy()
        );
        ProviderLoadBalancer loadBalancer = loadBalancers.get(
                resolved.loadBalancer()
        );
        if (loadBalancer == null) {
            throw new IllegalStateException(
                    "GATEWAY_LOAD_BALANCER_UNAVAILABLE: "
                            + resolved.loadBalancer()
            );
        }
        return loadBalancer.select(serviceKey, result.candidates());
    }

    private ResolvedPolicies resolve(
            ProviderServiceKey serviceKey,
            Set<String> policyRefs) {
        LoadBalancerType loadBalancer = LoadBalancerType.ROUND_ROBIN;
        ProviderSelectionPolicy selection = policies.apply(serviceKey);
        for (String policyRef : policyRefs) {
            RuntimeProviderPolicy policy = runtimePolicies.get().get(
                    policyRef
            );
            if (policy == null) {
                continue;
            }
            if (policy.type() == RuntimeProviderPolicy.Type.LOAD_BALANCE) {
                loadBalancer = policy.loadBalancer();
            } else {
                selection = policy.selectionPolicy();
            }
        }
        return new ResolvedPolicies(loadBalancer, selection);
    }

    public static Map<LoadBalancerType, ProviderLoadBalancer>
            defaultLoadBalancers() {
        EnumMap<LoadBalancerType, ProviderLoadBalancer> result =
                new EnumMap<>(LoadBalancerType.class);
        for (LoadBalancerType type : LoadBalancerType.values()) {
            result.put(type, ProviderLoadBalancers.create(type));
        }
        return Map.copyOf(result);
    }

    private record ResolvedPolicies(
            LoadBalancerType loadBalancer,
            ProviderSelectionPolicy selectionPolicy
    ) {
    }
}
