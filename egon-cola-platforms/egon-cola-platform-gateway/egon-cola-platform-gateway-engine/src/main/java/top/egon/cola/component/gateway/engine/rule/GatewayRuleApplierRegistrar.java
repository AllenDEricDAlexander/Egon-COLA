package top.egon.cola.component.gateway.engine.rule;

import top.egon.cola.component.ddc.configuration.refresh.DdcConfigApplierRegistry;

import java.util.Objects;

public final class GatewayRuleApplierRegistrar {

    private GatewayRuleApplierRegistrar() {
    }

    public static void register(
            DdcConfigApplierRegistry registry,
            GatewayRuleActivationApplier activation,
            GatewayRuleChunkStore chunks) {
        Objects.requireNonNull(registry, "registry").registerExact(
                GatewayRuleActivationApplier.ACTIVE_CONFIG_KEY,
                Objects.requireNonNull(activation, "activation")
        );
        registry.registerPrefix(
                "gateway.rules.chunk.",
                Objects.requireNonNull(chunks, "chunks")
        );
    }
}
