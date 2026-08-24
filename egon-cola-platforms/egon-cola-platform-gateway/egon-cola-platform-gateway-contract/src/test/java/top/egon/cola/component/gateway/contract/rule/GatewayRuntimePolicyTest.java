package top.egon.cola.component.gateway.contract.rule;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayRuntimePolicyTest {

    @Test
    void preservesExplicitNullConfigurationValuesInAnImmutableMap() {
        Map<String, Object> configuration = new LinkedHashMap<>();
        configuration.put("authenticationMode", "NONE");
        configuration.put("credentialRecoveryProviderId", null);

        GatewayRuntimePolicy policy = new GatewayRuntimePolicy(
                "public-policy",
                "SECURITY",
                "OPERATION",
                configuration
        );

        assertEquals("NONE", policy.configuration().get("authenticationMode"));
        assertTrue(policy.configuration().containsKey(
                "credentialRecoveryProviderId"
        ));
        assertNull(policy.configuration().get("credentialRecoveryProviderId"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> policy.configuration().put("future", true)
        );
    }
}
