package top.egon.cola.component.gateway.engine.balance;

import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;

import java.util.List;

@FunctionalInterface
public interface ProviderLoadBalancer {

    ProviderSelectionHandle select(
            ProviderServiceKey serviceKey,
            List<ProviderInstance> candidates
    );
}
