package top.egon.cola.component.gateway.engine.discovery;

import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.engine.balance.ProviderLoadBalancer;
import top.egon.cola.component.gateway.engine.balance.ProviderSelectionHandle;
import top.egon.cola.component.gateway.engine.http.ProviderSelector;

import java.util.Objects;

public final class DirectoryProviderSelector implements ProviderSelector {

    private final ProviderDirectory directory;

    private final ProviderLoadBalancer loadBalancer;

    public DirectoryProviderSelector(
            ProviderDirectory directory,
            ProviderLoadBalancer loadBalancer) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.loadBalancer = Objects.requireNonNull(
                loadBalancer,
                "loadBalancer"
        );
    }

    @Override
    public ProviderInstance select(ProviderServiceKey serviceKey) {
        ProviderSelectionHandle handle = loadBalancer.select(
                serviceKey,
                directory.available(serviceKey)
        );
        try {
            return handle.instance();
        } finally {
            handle.close();
        }
    }
}
