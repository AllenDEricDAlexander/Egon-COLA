package top.egon.cola.component.gateway.engine.http;

import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;

@FunctionalInterface
public interface ProviderSelector {

    ProviderInstance select(ProviderServiceKey serviceKey);
}
