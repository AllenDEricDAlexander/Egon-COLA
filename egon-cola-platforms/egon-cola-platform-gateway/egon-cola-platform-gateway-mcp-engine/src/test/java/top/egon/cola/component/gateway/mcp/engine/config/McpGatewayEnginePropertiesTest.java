package top.egon.cola.component.gateway.mcp.engine.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class McpGatewayEnginePropertiesTest {

    private static final String PREFIX = "egon.cola.component.gateway.mcp-engine.";
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void bindsImmutableBootstrapDefaultsWithoutRemappingLegacyMcpKeys() {
        runner.withPropertyValues(validValues())
                .withPropertyValues("egon.cola.component.gateway.engine.mcp.issuer=https://issuer.example.test")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    var properties = context.getBean(McpGatewayEngineProperties.class);
                    assertEquals(18084, properties.listener().port());
                    assertEquals(18085, properties.managementPort());
                    assertEquals("mcp-node-1", properties.nodeId());
                    assertEquals(Duration.ofSeconds(5), properties.outbound().requestTimeout());
                    assertTrue(McpGatewayEngineProperties.class.isRecord());
                    assertEquals("data/mcp-node-1", properties.dataDirectory());
                });
    }

    @Test
    void rejectsMissingIdentityBeforeAnyListenerCanStart() {
        for (String identity : List.of("gateway-group-code", "env", "namespace",
                "node-id", "instance-id", "data-directory")) {
            runner.withPropertyValues(validValues()).withPropertyValues(PREFIX + identity + "=")
                    .run(context -> assertNotNull(context.getStartupFailure(), identity));
        }
        runner.withPropertyValues(PREFIX + "gateway-group-code=orders",
                        PREFIX + "env=local", PREFIX + "namespace=default",
                        PREFIX + "instance-id=instance-1", PREFIX + "data-directory=data/mcp",
                        PREFIX + "listener.tls.development-plaintext=true",
                        PREFIX + "outbound.rpc-tls.development-plaintext=true")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    void rejectsUnsafePathsPortsDurationsAndConfigurableRoles() {
        for (String invalid : List.of(
                "data-directory=/", "data-directory=.", "data-directory=../outside",
                "listener.port=-1", "management-port=65536", "management-port=18084",
                "listener.drain-timeout=0s", "outbound.request-timeout=-1s",
                "outbound.max-connections=0", "active-health.maximum-concurrency=0",
                "role=API_RPC")) {
            runner.withPropertyValues(validValues()).withPropertyValues(PREFIX + invalid)
                    .run(context -> assertNotNull(context.getStartupFailure(), invalid));
        }
    }

    @Test
    void requiresExplicitPlaintextOrCompleteTlsConfiguration() {
        runner.withPropertyValues(validValues())
                .withPropertyValues(PREFIX + "listener.tls.development-plaintext=false")
                .run(context -> assertNotNull(context.getStartupFailure()));
        runner.withPropertyValues(validValues())
                .withPropertyValues(PREFIX + "listener.tls.enabled=true",
                        PREFIX + "listener.tls.development-plaintext=false")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }

    private String[] validValues() {
        return new String[]{
                PREFIX + "gateway-group-code=orders", PREFIX + "env=local",
                PREFIX + "namespace=default", PREFIX + "node-id=mcp-node-1",
                PREFIX + "instance-id=mcp-instance-1", PREFIX + "data-directory=./data/mcp-node-1",
                PREFIX + "listener.tls.development-plaintext=true",
                PREFIX + "outbound.rpc-tls.development-plaintext=true"
        };
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(McpGatewayEngineProperties.class)
    static class PropertiesConfiguration {
    }
}
