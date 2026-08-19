package top.egon.cola.component.gateway.engine;

import top.egon.cola.component.gateway.engine.common.config.GatewayEngineRuntimeProperties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayEngineRuntimePropertiesTest {

    @Test
    void defaultsKeepPhysicalZonesSeparateAndKafkaOptIn() {
        GatewayEngineRuntimeProperties properties =
                new GatewayEngineRuntimeProperties();

        assertTrue(properties.getHttp().isPublicEnabled());
        assertTrue(properties.getHttp().isInternalEnabled());
        assertNotEquals(
                properties.getHttp().getPublicPort(),
                properties.getHttp().getInternalPort()
        );
        assertTrue(properties.getRpc().isEnabled());
        assertEquals("egon-gateway-rpc", properties.getRpc().getServiceName());
        assertFalse(properties.getKafka().isEnabled());
        assertFalse(properties.getActiveHealth().isEnabled());
        assertEquals(
                2L * 1024 * 1024,
                properties.getHttp().getMaxBodyBytes()
        );
    }
}
