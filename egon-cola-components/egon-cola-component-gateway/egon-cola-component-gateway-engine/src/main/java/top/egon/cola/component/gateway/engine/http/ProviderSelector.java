package top.egon.cola.component.gateway.engine.http;

import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.engine.balance.ProviderSelectionHandle;

import java.util.Set;

@FunctionalInterface
public interface ProviderSelector {

    ProviderSelectionHandle select(ProviderServiceKey serviceKey);

    default ProviderSelectionHandle select(
            ProviderServiceKey serviceKey,
            Set<String> policyRefs) {
        return select(serviceKey);
    }
}
