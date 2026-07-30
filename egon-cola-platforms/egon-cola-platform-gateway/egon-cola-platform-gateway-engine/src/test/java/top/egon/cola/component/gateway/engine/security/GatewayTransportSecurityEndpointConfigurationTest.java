package top.egon.cola.component.gateway.engine.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayTransportSecurityEndpointConfigurationTest {

    @Test
    void requiresExplicitOptIn() {
        ConditionalOnProperty condition =
                GatewayTransportSecurityEndpointConfiguration.class
                        .getAnnotation(ConditionalOnProperty.class);

        assertEquals(
                "egon.cola.component.gateway.engine.tls-reload",
                condition.prefix()
        );
        assertTrue(Arrays.asList(condition.name()).contains("enabled"));
        assertEquals("true", condition.havingValue());
        assertFalse(condition.matchIfMissing());
    }
}
