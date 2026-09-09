package top.egon.cola.component.yuheng.mcp.engine.mcp.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayEngineRbac3ConfigurationTest {

    @Test
    void declaresTheMcpDownstreamAuthorizationClient() {
        YamlPropertiesFactoryBean loader = new YamlPropertiesFactoryBean();
        loader.setResources(new ClassPathResource("application.yml"));
        Properties properties = loader.getObject();

        assertEquals("${YUHENG_MCP_TIANQUAN_JIANSHEN_ENABLED:false}", value(
                properties,
                "egon.cola.platform.tianquan.jianshen.enabled"
        ));
        assertEquals("false", value(
                properties,
                "egon.cola.platform.tianquan.jianshen.register-filter"
        ));
        assertEquals("${YUHENG_MCP_TIANQUAN_JIANSHEN_SYSTEM_CODE:mock-backend}", value(
                properties,
                "egon.cola.platform.tianquan.jianshen.system-code"
        ));
        assertEquals("${YUHENG_MCP_TIANQUAN_JIANSHEN_REDIS_ADDRESS:redis://127.0.0.1:6379}",
                value(properties,
                        "egon.cola.platform.tianquan.jianshen.runtime.redis-address"));
        assertEquals("${YUHENG_MCP_TIANQUAN_JIANSHEN_REDIS_PASSWORD_FILE:}", value(
                properties,
                "egon.cola.platform.tianquan.jianshen.runtime.password-file"
        ));
        assertEquals("${YUHENG_MCP_TIANQUAN_JIANSHEN_AUTHORIZATION_ENDPOINT:}", value(
                properties,
                "egon.cola.platform.tianquan.jianshen.authorization.endpoint"
        ));
    }

    private String value(Properties properties, String key) {
        return properties.getProperty(key);
    }
}
