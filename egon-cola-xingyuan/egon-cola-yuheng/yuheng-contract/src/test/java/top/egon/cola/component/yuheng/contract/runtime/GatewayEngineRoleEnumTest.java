package top.egon.cola.component.yuheng.contract.runtime;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayEngineRoleEnumTest {

    @Test
    void containsExactlyApiRpcAndMcp() {
        assertArrayEquals(
                new String[]{"API_RPC", "MCP"},
                Arrays.stream(GatewayEngineRoleEnum.values())
                        .map(Enum::name)
                        .toArray(String[]::new)
        );
    }

    @Test
    void parsesCanonicalMetadata() {
        for (GatewayEngineRoleEnum role : GatewayEngineRoleEnum.values()) {
            assertEquals(Optional.of(role), GatewayEngineRoleEnum.fromWire(role.name()));
            assertEquals(Optional.of(role), GatewayEngineRoleEnum.fromWire(" " + role.name() + " "));
        }
    }

    @Test
    void rejectsUnknownRole() {
        for (String value : Arrays.asList(null, "", " ", "api_rpc", "mcp", "API", "RPC", "COMBINED")) {
            assertTrue(GatewayEngineRoleEnum.fromWire(value).isEmpty(), "unexpected role: " + value);
        }
    }
}
