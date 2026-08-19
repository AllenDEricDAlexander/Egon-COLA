package top.egon.cola.component.gateway.engine;

import top.egon.cola.component.gateway.engine.bootstrap.config.GatewayEngineConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import javax.sql.DataSource;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNull;

class GatewayEngineTaskStoreConfigurationTest {

    @Test
    void taskStoreDependsDirectlyOnTheConfiguredDataSource() throws Exception {
        Method factory = GatewayEngineConfiguration.class.getMethod(
                "gatewayMcpRuntimeTaskStore",
                DataSource.class,
                ObjectMapper.class
        );

        assertNull(factory.getAnnotation(ConditionalOnBean.class));
    }
}
