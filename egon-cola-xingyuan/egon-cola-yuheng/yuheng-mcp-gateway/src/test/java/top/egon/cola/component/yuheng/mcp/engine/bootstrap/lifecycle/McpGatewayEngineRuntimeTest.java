package top.egon.cola.component.yuheng.mcp.engine.bootstrap.lifecycle;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import top.egon.cola.component.yuheng.mcp.engine.http.service.McpGatewayHttpServer;
import top.egon.cola.component.yuheng.mcp.engine.mcp.service.McpRuntimeHealthIndicator;
import top.egon.cola.component.yuheng.mcp.engine.rule.domain.McpGatewayCompiledRulesDTO;
import top.egon.cola.component.yuheng.runtime.provider.service.ProviderDirectory;
import top.egon.cola.component.yuheng.runtime.rule.domain.GatewayRuleRuntimeStatus;
import top.egon.cola.component.yuheng.runtime.rule.service.GatewayRuleActivationApplier;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpGatewayEngineRuntimeTest {

    @Test
    void retainsReadinessOfAServingSnapshotWhenHealthIsDegraded() {
        assertReadiness("DEGRADED", true);
    }

    @Test
    void stillRejectsUnavailableAndUnknownHealth() {
        assertReadiness("UP", true);
        assertReadiness("DOWN", false);
        assertReadiness("OUT_OF_SERVICE", false);
        assertReadiness("UNKNOWN", false);
    }

    @SuppressWarnings("unchecked")
    private void assertReadiness(String healthStatus, boolean expected) {
        var server = mock(McpGatewayHttpServer.class);
        when(server.accepting()).thenReturn(true);
        GatewayRuleActivationApplier<McpGatewayCompiledRulesDTO> activation = mock(GatewayRuleActivationApplier.class);
        var rules = mock(McpGatewayCompiledRulesDTO.class);
        when(rules.providerServices()).thenReturn(Set.of());
        when(activation.active()).thenReturn(rules);
        var status = mock(GatewayRuleRuntimeStatus.class);
        when(status.ready()).thenReturn(true);
        when(activation.status()).thenReturn(status);
        var directory = mock(ProviderDirectory.class);
        when(directory.allAvailable(anySet())).thenReturn(true);
        var health = mock(McpRuntimeHealthIndicator.class);
        when(health.health()).thenReturn(Health.status(healthStatus).build());
        var runtime = new McpGatewayEngineRuntime(server, activation, directory, health);
        try {
            runtime.start();
            assertEquals(expected, runtime.ready(), healthStatus);
        } finally {
            runtime.stop();
        }
    }
}
