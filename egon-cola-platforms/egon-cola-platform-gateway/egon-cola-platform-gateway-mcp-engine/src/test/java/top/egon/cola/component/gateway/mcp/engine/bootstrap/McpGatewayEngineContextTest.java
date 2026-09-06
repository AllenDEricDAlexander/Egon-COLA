package top.egon.cola.component.gateway.mcp.engine.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.redisson.api.RedissonClient;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.api.extension.DdcInstanceMetadataContributor;
import top.egon.cola.component.ddc.api.refresh.DdcConfigApplierRegistry;
import top.egon.cola.component.gateway.mcp.engine.bootstrap.config.McpGatewayEngineConfiguration;
import top.egon.cola.component.gateway.mcp.engine.bootstrap.lifecycle.McpGatewayEngineRuntime;
import top.egon.cola.component.gateway.mcp.engine.http.service.McpGatewayHttpServer;
import top.egon.cola.component.gateway.mcp.engine.rule.service.McpGatewayRuleCompilerStrategy;
import top.egon.cola.component.gateway.runtime.operation.service.EngineGatewayOperationInvoker;
import top.egon.cola.component.gateway.runtime.rule.service.GatewayRuleCompilerStrategy;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class McpGatewayEngineContextTest {

    @TempDir
    Path dataDirectory;

    @Test
    void startsMcpOnlyContextWithNamedDirectInvokerAndFixedRole() {
        runner().withBean("dataSource", DataSource.class, () -> mock(DataSource.class))
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertEquals(1, context.getBeansOfType(McpGatewayEngineRuntime.class).size());
                    assertEquals(1, context.getBeansOfType(McpGatewayHttpServer.class).size());
                    assertEquals(1, context.getBeansOfType(GatewayRuleCompilerStrategy.class).size());
                    assertSame(context.getBean("gatewayRuleCompilerStrategy"),
                            context.getBean("mcpGatewayRuleCompilerStrategy"));
                    assertInstanceOf(McpGatewayRuleCompilerStrategy.class, context.getBean("gatewayRuleCompilerStrategy"));
                    assertInstanceOf(EngineGatewayOperationInvoker.class, context.getBean("gatewayOperationInvoker"));
                    assertFalse(context.containsBean("gatewayRpcServer"));
                    assertFalse(context.containsBean("gatewayRpcSlotRuntime"));
                    assertFalse(context.containsBean("gatewayHttpServer"));
                    var metadata = context.getBean("gatewayRuntimeMetadata", DdcInstanceMetadataContributor.class);
                    assertEquals("MCP", metadata.metadata().get("gateway.engine.role"));
                    assertTrue(context.getBean(McpGatewayEngineRuntime.class).running());
                    assertFalse(context.getBean(McpGatewayEngineRuntime.class).ready());
                });
    }

    @Test
    void missingDurableStoreFailsContextInsteadOfStartingPartialRuntime() {
        runner().run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    void generatedRuntimeConstructorsPreserveDependencyQualifiers() {
        for (Class<?> type : new Class<?>[]{McpGatewayEngineRuntime.class, McpGatewayHttpServer.class,
                top.egon.cola.component.gateway.mcp.engine.http.service.McpGatewayHttpDataPlaneHandlerAdapter.class}) {
            assertEquals(1, type.getConstructors().length);
            for (var parameter : type.getConstructors()[0].getParameters()) {
                assertNotNull(parameter.getAnnotation(org.springframework.beans.factory.annotation.Qualifier.class),
                        type.getSimpleName() + ": " + parameter.getName());
            }
        }
    }

    @Test
    void profilesHaveIdenticalKeysAndPreserveLegacyProtocolPrefix() {
        Properties base = yaml("application.yml");
        Properties operations = yaml("application-operations.yml");
        assertEquals(base.stringPropertyNames(), operations.stringPropertyNames());
        assertEquals("${GATEWAY_MCP_ENABLED:true}",
                base.getProperty("egon.cola.component.gateway.engine.mcp.enabled"));
        assertEquals("MCP", base.getProperty(
                "egon.cola.component.ddc.registry.http.metadata.gateway.engine.role"));
        assertFalse(base.stringPropertyNames().stream().anyMatch(key ->
                key.startsWith("egon.cola.component.gateway.engine.http.")
                        || key.startsWith("egon.cola.component.gateway.engine.rpc.")));
        assertEquals("${DDC_APP_CODE:gme}", base.getProperty("egon.cola.component.ddc.app-code"));
        assertTrue(base.getProperty("egon.cola.component.ddc.rpc.auth.runtime.secret-key")
                .contains("GATEWAY_MCP_DDC_RPC_RUNTIME_SECRET_KEY"));
    }

    private ApplicationContextRunner runner() {
        String prefix = "egon.cola.component.gateway.mcp-engine.";
        return new ApplicationContextRunner().withUserConfiguration(McpGatewayEngineConfiguration.class)
                .withPropertyValues(prefix + "gateway-group-code=orders", prefix + "env=local",
                        prefix + "namespace=default", prefix + "node-id=mcp-test", prefix + "instance-id=mcp-test",
                        prefix + "data-directory=" + dataDirectory,
                        prefix + "listener.enabled=false", prefix + "listener.port=0",
                        prefix + "listener.tls.development-plaintext=true",
                        prefix + "outbound.rpc-tls.development-plaintext=true")
                .withBean("objectMapper", ObjectMapper.class, () -> new ObjectMapper().findAndRegisterModules())
                .withBean("meterRegistry", SimpleMeterRegistry.class, SimpleMeterRegistry::new)
                .withBean("observationRegistry", ObservationRegistry.class, ObservationRegistry::create)
                .withBean("ddcServiceRegistryClient", DdcServiceRegistryClient.class,
                        () -> mock(DdcServiceRegistryClient.class))
                .withBean("ddcConfigApplierRegistry", DdcConfigApplierRegistry.class,
                        () -> mock(DdcConfigApplierRegistry.class))
                .withBean("gatewayMcpRedissonClient", RedissonClient.class, () -> mock(RedissonClient.class));
    }

    private Properties yaml(String resource) {
        var loader = new YamlPropertiesFactoryBean();
        loader.setResources(new ClassPathResource(resource));
        return loader.getObject();
    }

    @Test
    void ownsExecutableAndRuntimeWithoutApiIngressClasses() {
        for (String name : new String[] {
                "McpGatewayEngineApplication",
                "bootstrap.config.McpGatewayEngineConfiguration",
                "bootstrap.lifecycle.McpGatewayEngineRuntime",
                "http.service.McpGatewayHttpServer",
                "http.service.McpGatewayHttpDataPlaneHandlerAdapter"
        }) {
            assertDoesNotThrow(() -> Class.forName(
                    "top.egon.cola.component.gateway.mcp.engine." + name));
        }
        assertThrows(ClassNotFoundException.class, () -> Class.forName(
                "top.egon.cola.component.gateway.engine.rpc.service.RpcGatewayServer"));
    }
}
