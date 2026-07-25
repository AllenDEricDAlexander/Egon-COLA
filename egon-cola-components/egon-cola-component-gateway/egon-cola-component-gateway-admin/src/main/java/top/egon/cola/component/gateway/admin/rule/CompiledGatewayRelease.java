package top.egon.cola.component.gateway.admin.rule;

import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;

import java.util.Map;
import java.util.Objects;

public record CompiledGatewayRelease(
        GatewayRuleSnapshot snapshot,
        String snapshotJson,
        GatewayRuleActivation activation,
        String activationJson,
        Map<String, String> chunkValues
) {

    public CompiledGatewayRelease {
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
        snapshotJson = Objects.requireNonNull(snapshotJson, "snapshotJson");
        activation = Objects.requireNonNull(activation, "activation");
        activationJson = Objects.requireNonNull(
                activationJson,
                "activationJson"
        );
        chunkValues = Map.copyOf(Objects.requireNonNull(
                chunkValues,
                "chunkValues"
        ));
    }
}
