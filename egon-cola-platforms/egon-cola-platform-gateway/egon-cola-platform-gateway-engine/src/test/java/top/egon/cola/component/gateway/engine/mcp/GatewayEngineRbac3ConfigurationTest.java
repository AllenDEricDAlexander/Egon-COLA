package top.egon.cola.component.gateway.engine.mcp;

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

        assertEquals("${GATEWAY_MCP_RBAC3_ENABLED:false}", value(
                properties,
                "egon.cola.platform.rbac3.enabled"
        ));
        assertEquals("false", value(
                properties,
                "egon.cola.platform.rbac3.register-filter"
        ));
        assertEquals("${GATEWAY_MCP_RBAC3_SYSTEM_CODE:mock-backend}", value(
                properties,
                "egon.cola.platform.rbac3.system-code"
        ));
        assertEquals("${GATEWAY_MCP_RBAC3_REDIS_ADDRESS:redis://127.0.0.1:6379}",
                value(properties,
                        "egon.cola.platform.rbac3.runtime.redis-address"));
        assertEquals("${GATEWAY_MCP_RBAC3_REDIS_PASSWORD_FILE:}", value(
                properties,
                "egon.cola.platform.rbac3.runtime.password-file"
        ));
        assertEquals("${GATEWAY_MCP_RBAC3_AUTHORIZATION_ENDPOINT:}", value(
                properties,
                "egon.cola.platform.rbac3.authorization.endpoint"
        ));
        assertEquals("${GATEWAY_MCP_RBAC3_SERVICE_CREDENTIAL_FILE:}", value(
                properties,
                "egon.cola.platform.rbac3.authorization.service-credential-file"
        ));
    }

    private String value(Properties properties, String key) {
        return properties.getProperty(key);
    }
}
