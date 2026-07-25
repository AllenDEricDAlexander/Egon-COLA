package top.egon.cola.component.gateway.engine.rule;

import java.time.Instant;

public record GatewayRuleRuntimeStatus(
        String activeReleaseId,
        long activeDdcVersion,
        String ruleSchemaVersion,
        String ruleContentSha256,
        String artifactSha256,
        GatewayRuleApplyStage lastStage,
        String lastError,
        int routeCount,
        int operationCount,
        int providerServiceCount,
        int stagingChunkCount,
        boolean ready,
        boolean degraded,
        Instant updatedAt
) {

    public static GatewayRuleRuntimeStatus empty() {
        return new GatewayRuleRuntimeStatus(
                null,
                0,
                null,
                null,
                null,
                GatewayRuleApplyStage.NEVER_APPLIED,
                null,
                0,
                0,
                0,
                0,
                false,
                false,
                Instant.EPOCH
        );
    }
}
