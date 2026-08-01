package top.egon.cola.component.gateway.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayAdminApplicationConfigurationTest {

    @Test
    void excludesGenericRedissonSpringDataAutoConfiguration() {
        YamlPropertiesFactoryBean loader = new YamlPropertiesFactoryBean();
        loader.setResources(new ClassPathResource("application.yml"));

        assertEquals(
                "org.redisson.spring.starter.RedissonAutoConfigurationV2",
                loader.getObject().getProperty(
                        "spring.autoconfigure.exclude[0]"
                )
        );
    }
}
