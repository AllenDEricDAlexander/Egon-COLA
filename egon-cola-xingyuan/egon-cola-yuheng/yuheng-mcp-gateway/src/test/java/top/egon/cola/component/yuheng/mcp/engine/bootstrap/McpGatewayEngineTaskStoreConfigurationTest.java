package top.egon.cola.component.yuheng.mcp.engine.bootstrap;

import top.egon.cola.component.yuheng.mcp.engine.bootstrap.config.McpGatewayEngineConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import top.egon.cola.component.yuheng.mcp.engine.mcp.adapter.support.McpGatewayPersistenceContext;
import top.egon.cola.component.yuheng.mcp.engine.mcp.converter.McpTaskPersistenceConverter;
import top.egon.cola.component.yuheng.mcp.engine.mcp.repository.McpTaskPersistenceRepository;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNull;

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
}
