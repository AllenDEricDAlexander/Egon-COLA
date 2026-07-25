package top.egon.cola.component.gateway.engine.discovery;

import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;

@FunctionalInterface
public interface ProviderActiveHealthProbe {

    Mono<Boolean> probe(
            ProviderInstance instance,
            ActiveHealthProbePolicy policy);
}
