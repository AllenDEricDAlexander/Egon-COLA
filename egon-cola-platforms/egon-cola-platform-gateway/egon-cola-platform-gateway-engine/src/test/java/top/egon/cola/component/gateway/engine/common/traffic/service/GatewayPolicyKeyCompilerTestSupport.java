package top.egon.cola.component.gateway.engine.common.traffic.service;

import top.egon.cola.component.gateway.engine.rule.adapter.json.GatewayRuleJsonCodec;

import java.nio.charset.StandardCharsets;

final class GatewayPolicyKeyCompilerTestSupport {

    private GatewayPolicyKeyCompilerTestSupport() {
    }

    static String hash(String value) {
        return GatewayRuleJsonCodec.sha256(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }
}
