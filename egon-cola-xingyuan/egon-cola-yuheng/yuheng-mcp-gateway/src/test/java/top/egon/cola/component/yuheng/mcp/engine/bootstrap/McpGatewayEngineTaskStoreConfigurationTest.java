package top.egon.cola.component.yuheng.mcp.engine.bootstrap;

import top.egon.cola.component.yuheng.mcp.engine.bootstrap.config.McpGatewayEngineConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import javax.sql.DataSource;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNull;

class McpGatewayEngineTaskStoreConfigurationTest {

    @Test
    void taskStoreDependsDirectlyOnTheConfiguredDataSource() throws Exception {
        Method factory = McpGatewayEngineConfiguration.class.getMethod(
                "gatewayMcpRuntimeTaskStore",
                DataSource.class,
                ObjectMapper.class
        );

        assertNull(factory.getAnnotation(ConditionalOnBean.class));
    }
}
