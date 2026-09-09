package top.egon.cola.component.yuheng.admin.runtime.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.tianshu.model.management.DdcManagementConfigClientInstance;
import top.egon.cola.component.yuheng.contract.runtime.GatewayEngineRoleEnum;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayEngineRoleConsistencyStrategyTest {

    private final GatewayEngineRoleConsistencyStrategy strategy = new GatewayEngineRoleConsistencyStrategy();

    @Test
    void parsesOnlyExplicitCanonicalRoles() {
        assertThat(strategy.roleOf(node(Map.of("yuheng.engine.role", " API_RPC "))))
                .contains(GatewayEngineRoleEnum.API_RPC);
        assertThat(strategy.roleOf(node(Map.of("yuheng.engine.role", "MCP"))))
                .contains(GatewayEngineRoleEnum.MCP);
        for (String unknown : List.of("", " ", "mcp", "COMBINED")) {
            assertThat(strategy.roleOf(node(Map.of("yuheng.engine.role", unknown)))).isEmpty();
        }
        assertThat(strategy.roleOf(node(null))).isEmpty();
        assertThat(strategy.roleOf(null)).isEmpty();
    }

    @Test
    void countsReplicasOnceAndNeverInfersRoleFromNodeName() {
        var api = node(Map.of("yuheng.engine.role", "API_RPC"));
        var mcp = node(Map.of("yuheng.engine.role", "MCP"));
        assertThat(strategy.missingRoles(null)).containsExactlyInAnyOrder(GatewayEngineRoleEnum.values());
        assertThat(strategy.missingRoles(List.of(api, api))).containsExactly(GatewayEngineRoleEnum.MCP);
        assertThat(strategy.missingRoles(List.of(api, mcp, mcp))).isEmpty();
        assertThat(strategy.hasUnknownRole(List.of(api, mcp))).isFalse();
        assertThat(strategy.hasUnknownRole(List.of(api, node(null)))).isTrue();
        assertThat(strategy.missingRoles(List.of(node(Map.of("role", "MCP")))))
                .containsExactlyInAnyOrder(GatewayEngineRoleEnum.values());
    }

    private DdcManagementConfigClientInstance node(Map<String, String> metadata) {
        return new DdcManagementConfigClientInstance("infra", "test", "ge", "yuheng-mcp-gateway",
                "lease", "yuheng-mcp-gateway", 18084, "CONFIG_CLIENT", "ONLINE",
                null, null, null, metadata);
    }
}
