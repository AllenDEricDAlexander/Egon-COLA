package top.egon.cola.component.gateway.engine.discovery;

import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.engine.balance.ProviderLoadBalancer;
import top.egon.cola.component.gateway.engine.balance.ProviderSelectionHandle;
import top.egon.cola.component.gateway.engine.http.ProviderSelector;

import java.time.Clock;
import java.util.Objects;
import java.util.function.Function;

public final class DirectoryProviderSelector implements ProviderSelector {

    private final ProviderDirectory directory;

    private final ProviderLoadBalancer loadBalancer;

    private final ProviderCandidateFilter candidateFilter;

    private final Function<ProviderServiceKey, ProviderSelectionPolicy> policies;

    public DirectoryProviderSelector(
            ProviderDirectory directory,
            ProviderLoadBalancer loadBalancer) {
        this(
                directory,
                loadBalancer,
                new ProviderCandidateFilter(Clock.systemUTC(), ignored -> true),
                key -> ProviderSelectionPolicy.defaults(
                        key.transport().equals("https")
                )
        );
    }

    public DirectoryProviderSelector(
            ProviderDirectory directory,
            ProviderLoadBalancer loadBalancer,
            ProviderCandidateFilter candidateFilter,
            Function<ProviderServiceKey, ProviderSelectionPolicy> policies) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.loadBalancer = Objects.requireNonNull(
                loadBalancer,
                "loadBalancer"
        );
        this.candidateFilter = Objects.requireNonNull(
                candidateFilter,
                "candidateFilter"
        );
        this.policies = Objects.requireNonNull(policies, "policies");
    }

    @Override
    public ProviderSelectionHandle select(ProviderServiceKey serviceKey) {
        ProviderCandidateFilterResult result = candidateFilter.filter(
                serviceKey,
                directory.instances(serviceKey),
                policies.apply(serviceKey)
        );
        return loadBalancer.select(serviceKey, result.candidates());
    }
}
