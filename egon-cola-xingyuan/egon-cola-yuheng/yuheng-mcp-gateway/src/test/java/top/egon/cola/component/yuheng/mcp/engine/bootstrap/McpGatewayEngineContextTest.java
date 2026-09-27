package top.egon.cola.component.yuheng.mcp.engine.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.MDC;
import org.springframework.transaction.PlatformTransactionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.redisson.api.RedissonClient;
import top.egon.cola.component.tianshu.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.tianshu.api.extension.DdcInstanceMetadataContributor;
import top.egon.cola.component.tianshu.api.refresh.DdcConfigApplierRegistry;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.yuheng.mcp.engine.bootstrap.config.McpGatewayEngineConfiguration;
import top.egon.cola.component.yuheng.mcp.engine.bootstrap.lifecycle.McpGatewayEngineRuntime;
import top.egon.cola.component.yuheng.mcp.engine.config.McpPersistenceContextComponent;
import top.egon.cola.component.yuheng.mcp.engine.config.McpPersistenceConfiguration;
import top.egon.cola.component.yuheng.mcp.engine.config.McpPersistenceProperties;
import top.egon.cola.component.yuheng.mcp.engine.http.service.McpGatewayHttpServer;
import top.egon.cola.component.yuheng.mcp.engine.mcp.converter.McpTaskPersistenceConverter;
import top.egon.cola.component.yuheng.mcp.engine.mcp.dao.McpTaskDAO;
import top.egon.cola.component.yuheng.mcp.engine.mcp.repository.McpTaskPersistenceRepository;
import top.egon.cola.component.yuheng.mcp.engine.rule.service.McpGatewayRuleCompilerStrategy;
import top.egon.cola.component.yuheng.runtime.operation.service.EngineGatewayOperationInvoker;
import top.egon.cola.component.yuheng.runtime.rule.service.GatewayRuleCompilerStrategy;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.List;
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
                    assertEquals("MCP", metadata.metadata().get("yuheng.engine.role"));
                    assertTrue(context.getBean(McpGatewayEngineRuntime.class).running());
                    assertFalse(context.getBean(McpGatewayEngineRuntime.class).ready());
                });
    }

    @Test
    void missingDurableStoreFailsContextInsteadOfStartingPartialRuntime() {
        baseRunner().run(context -> {
            assertNotNull(context.getStartupFailure());
            assertTrue(String.valueOf(context.getStartupFailure().getMessage())
                            .contains("mcpTaskPersistenceRepository"),
                    "the engine must fail closed on the missing guarded task boundary, not boot partially");
        });
    }

    @Test
    void productionPersistenceConfigurationBindsTheUnifiedIdentityAndRestoresMdc() {
        persistenceRunner(true).run(context -> {
            assertNull(context.getStartupFailure());
            McpPersistenceContextComponent persistence = context.getBean(
                    "mcpPersistenceContextComponent", McpPersistenceContextComponent.class);
            MDC.put("tenantId", "prior-tenant");
            MDC.put("userId", "prior-user");
            try {
                assertEquals("mcp-result", persistence.call(null, () -> {
                    assertEquals("9001", MDC.get("tenantId"));
                    assertEquals("mcp-persistence-test", MDC.get("userId"));
                    return "mcp-result";
                }));
                assertEquals("prior-tenant", MDC.get("tenantId"));
                assertEquals("prior-user", MDC.get("userId"));
            } catch (Exception failure) {
                throw new AssertionError("the production persistence identity context must call and restore", failure);
            } finally {
                MDC.clear();
            }
        });
    }

    @Test
    void productionPersistenceConfigurationRejectsAnOmittedDdlRoleFlag() {
        persistenceRunner(false).run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    void generatedRuntimeConstructorsPreserveDependencyQualifiers() {
        for (Class<?> type : new Class<?>[]{McpGatewayEngineRuntime.class, McpGatewayHttpServer.class,
                top.egon.cola.component.yuheng.mcp.engine.http.service.McpGatewayHttpDataPlaneHandlerAdapter.class}) {
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
        assertEquals("${YUHENG_MCP_ENABLED:true}",
                base.getProperty("egon.cola.component.yuheng.engine.mcp.enabled"));
        assertEquals("MCP", base.getProperty(
                "egon.cola.component.tianshu.registry.http.metadata.yuheng.engine.role"));
        assertFalse(base.stringPropertyNames().stream().anyMatch(key ->
                key.startsWith("egon.cola.component.yuheng.engine.http.")
                        || key.startsWith("egon.cola.component.yuheng.engine.rpc.")));
        assertEquals("${TIANSHU_APP_CODE:gme}", base.getProperty("egon.cola.component.tianshu.app-code"));
        assertTrue(base.getProperty("egon.cola.component.tianshu.rpc.auth.runtime.secret-key")
                .contains("YUHENG_MCP_TIANSHU_RPC_RUNTIME_SECRET_KEY"));
    }

    private ApplicationContextRunner runner() {
        return baseRunner()
                .withBean("mcpTaskDAO", McpTaskDAO.class, () -> mock(McpTaskDAO.class))
                .withBean("mcpTaskPersistenceRepository", McpTaskPersistenceRepository.class,
                        () -> mock(McpTaskPersistenceRepository.class))
                .withBean("mcpTaskPersistenceConverter", McpTaskPersistenceConverter.class,
                        () -> mock(McpTaskPersistenceConverter.class));
    }

    private ApplicationContextRunner baseRunner() {
        String prefix = "egon.cola.component.yuheng.mcp-engine.";
        return new ApplicationContextRunner().withUserConfiguration(McpGatewayEngineConfiguration.class)
                .withPropertyValues(prefix + "yuheng-group-code=orders", prefix + "env=local",
                        prefix + "namespace=default", prefix + "node-id=mcp-test", prefix + "instance-id=mcp-test",
                        prefix + "data-directory=" + dataDirectory,
                        prefix + "listener.enabled=false", prefix + "listener.port=0",
                        prefix + "listener.tls.development-plaintext=true",
                        prefix + "outbound.rpc-tls.development-plaintext=true")
                .withBean("mcpPersistenceContextComponent", McpPersistenceContextComponent.class,
                        () -> mock(McpPersistenceContextComponent.class))
                .withBean(McpPersistenceProperties.BEAN_NAME, McpPersistenceProperties.class,
                        McpGatewayEngineContextTest::persistenceProperties)
                .withBean("objectMapper", ObjectMapper.class, () -> new ObjectMapper().findAndRegisterModules())
                .withBean("meterRegistry", SimpleMeterRegistry.class, SimpleMeterRegistry::new)
                .withBean("observationRegistry", ObservationRegistry.class, ObservationRegistry::create)
                .withBean("ddcServiceRegistryClient", DdcServiceRegistryClient.class,
                        () -> mock(DdcServiceRegistryClient.class))
                .withBean("ddcConfigApplierRegistry", DdcConfigApplierRegistry.class,
                        () -> mock(DdcConfigApplierRegistry.class))
                .withBean("gatewayMcpRedissonClient", RedissonClient.class, () -> mock(RedissonClient.class));
    }

    private static McpPersistenceProperties persistenceProperties() {
        return new McpPersistenceProperties()
                .setTenantId(9001L)
                .setIdentityTenantId(9001L)
                .setServiceUserId("yuheng-mcp-engine-test")
                .setExpectedSchemaVersion("20260922_001")
                .setExpectedSchemaSha256("0".repeat(64))
                .setManagedDdlEnabled(false);
    }

    private Properties yaml(String resource) {
        var loader = new YamlPropertiesFactoryBean();
        loader.setResources(new ClassPathResource(resource));
        return loader.getObject();
    }

    private ApplicationContextRunner persistenceRunner(boolean includeManagedDdlFlag) {
        List<String> properties = new java.util.ArrayList<>(List.of(
                "yuheng.persistence.tenant-id=9001",
                "yuheng.persistence.identity-tenant-id=9001",
                "yuheng.persistence.service-user-id=mcp-persistence-test",
                "yuheng.persistence.expected-schema-version=20260922_001",
                "yuheng.persistence.expected-schema-sha256=" + "a".repeat(64),
                "egon.cola.component.mybatis-plus.ddl.enabled=false"));
        if (includeManagedDdlFlag) {
            properties.add("yuheng.persistence.managed-ddl-enabled=false");
        }
        return new ApplicationContextRunner()
                .withUserConfiguration(McpPersistenceConfiguration.class)
                .withPropertyValues(properties.toArray(String[]::new))
                .withBean("dataSource", DataSource.class, () -> mock(DataSource.class))
                .withBean("mcp-test-sql-session-factory", SqlSessionFactory.class,
                        () -> mock(SqlSessionFactory.class))
                .withBean("mcp-test-transaction-manager", PlatformTransactionManager.class,
                        () -> mock(PlatformTransactionManager.class))
                .withBean("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties",
                        EgonColaMybatisPlusProperties.class, EgonColaMybatisPlusProperties::new);
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
                    "top.egon.cola.component.yuheng.mcp.engine." + name));
        }
        assertThrows(ClassNotFoundException.class, () -> Class.forName(
                "top.egon.cola.component.yuheng.engine.rpc.service.RpcGatewayServer"));
    }
}
