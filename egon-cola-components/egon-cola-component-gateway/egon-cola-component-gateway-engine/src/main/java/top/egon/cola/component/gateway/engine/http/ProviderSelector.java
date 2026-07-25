package top.egon.cola.component.gateway.engine.http;

import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.engine.balance.ProviderSelectionHandle;

@FunctionalInterface
public interface ProviderSelector {

    ProviderSelectionHandle select(ProviderServiceKey serviceKey);
}
