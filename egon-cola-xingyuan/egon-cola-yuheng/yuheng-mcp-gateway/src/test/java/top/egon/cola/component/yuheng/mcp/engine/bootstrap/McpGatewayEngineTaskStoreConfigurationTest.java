package top.egon.cola.component.yuheng.mcp.engine.bootstrap;

import top.egon.cola.component.yuheng.mcp.engine.bootstrap.config.McpGatewayEngineConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.support.GenericApplicationContext;
import jakarta.validation.constraints.NotNull;
import top.egon.cola.component.yuheng.mcp.engine.config.McpPersistenceProperties;
import top.egon.cola.component.yuheng.mcp.engine.McpGatewayEngineApplication;
import top.egon.cola.component.yuheng.mcp.engine.mcp.dao.McpApprovalDAO;
import top.egon.cola.component.yuheng.mcp.engine.mcp.dao.McpTaskDAO;

import top.egon.cola.component.yuheng.mcp.engine.mcp.adapter.support.McpGatewayPersistenceContext;
import top.egon.cola.component.yuheng.mcp.engine.mcp.converter.McpTaskPersistenceConverter;
import top.egon.cola.component.yuheng.mcp.engine.mcp.repository.McpTaskPersistenceRepository;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class McpGatewayEngineTaskStoreConfigurationTest {

    @Test
    void taskStoreDependsDirectlyOnTheGuardedPersistenceBoundary() throws Exception {
        Method factory = McpGatewayEngineConfiguration.class.getMethod(
                "gatewayMcpRuntimeTaskStore",
                McpTaskPersistenceRepository.class,
                McpTaskPersistenceConverter.class,
                McpGatewayPersistenceContext.class,
                int.class
        );

        assertNull(factory.getAnnotation(ConditionalOnBean.class));
    }

    @Test
    void executableScansProductionPersistenceBeansAndBothMappers() {
        SpringBootApplication application = McpGatewayEngineApplication.class
                .getAnnotation(SpringBootApplication.class);
        assertNotNull(application);
        assertTrue(java.util.List.of(application.scanBasePackages()).containsAll(java.util.List.of(
                "top.egon.cola.component.yuheng.mcp.engine.bootstrap",
                "top.egon.cola.component.yuheng.mcp.engine.config",
                "top.egon.cola.component.yuheng.mcp.engine.mcp")));

        MapperScan mapperScan = McpGatewayEngineApplication.class.getAnnotation(MapperScan.class);
        assertNotNull(mapperScan);
        assertTrue(java.util.List.of(mapperScan.basePackageClasses()).containsAll(java.util.List.of(
                McpTaskDAO.class, McpApprovalDAO.class)));
    }

    @Test
    void managedDdlRoleFlagIsRequiredInsteadOfSilentlyDefaultingToConsumer() throws Exception {
        var managedDdl = McpPersistenceProperties.class.getDeclaredField("managedDdlEnabled");
        assertEquals(Boolean.class, managedDdl.getType());
        assertNotNull(managedDdl.getAnnotation(NotNull.class));
    }

    @Test
    void executablePackageScanRegistersThePersistenceConfigurationAndAdapters() {
        SpringBootApplication application = McpGatewayEngineApplication.class
                .getAnnotation(SpringBootApplication.class);
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            new ClassPathBeanDefinitionScanner(context).scan(application.scanBasePackages());
            assertTrue(context.containsBeanDefinition("mcpPersistenceConfiguration"));
            assertTrue(context.containsBeanDefinition("mcpTaskPersistenceRepository"));
            assertTrue(context.containsBeanDefinition("mcpTaskPersistenceConverter"));
            assertTrue(context.containsBeanDefinition("mcpRuntimeTaskStore"));
            assertTrue(context.containsBeanDefinition("mcpApprovalPersistenceRepository"));
            assertTrue(context.containsBeanDefinition("mcpApprovalPersistenceConverter"));
            assertTrue(context.containsBeanDefinition("mcpApprovalAdapter"));
        }
    }
}
