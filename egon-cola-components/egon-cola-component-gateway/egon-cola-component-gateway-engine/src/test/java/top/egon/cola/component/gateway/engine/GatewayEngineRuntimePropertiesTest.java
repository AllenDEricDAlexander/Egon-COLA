package top.egon.cola.component.gateway.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertFalse(properties.getKafka().isEnabled());
    }
}
