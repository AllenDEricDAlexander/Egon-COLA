package top.egon.cola.component.gateway.test.rpc.provider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class RpcProviderApplicationConfigurationTest {

    @Test
    void excludesTheGenericRedissonAutoConfiguration() {
        YamlPropertiesFactoryBean loader = new YamlPropertiesFactoryBean();
        loader.setResources(new ClassPathResource("application.yml"));

        assertThat(loader.getObject().getProperty(
                "spring.autoconfigure.exclude[0]"
        )).isEqualTo(
                        "org.redisson.spring.starter."
                                + "RedissonAutoConfigurationV2"
                );
    }
}
