package top.egon.cola.component.tianshu.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.tianshu.admin.repository.DdcInstanceRepository;
import top.egon.cola.component.tianshu.admin.security.registration.DdcRegistrationCredentialVerifier;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcScopeGate;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterAutoConfiguration;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerStateReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DdcAdminRedisConfigTest {

    @Test
    void createsRegistrationVerifierWhenIdpServiceTokenVerifierComesFromAutoConfiguration() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        IdpStarterAutoConfiguration.class
                ))
                .withUserConfiguration(
                        TestInfrastructure.class,
                        DdcAdminRedisConfig.class
                )
                .withPropertyValues(
                        "egon.cola.platform.tianquan.shoubing.enabled=true",
                        "egon.cola.platform.tianquan.shoubing.issuer=https://tianquan-shoubing.example",
                        "egon.cola.platform.tianquan.shoubing.jwk-set-uri=https://tianquan-shoubing.example/oauth2/jwks",
                        "egon.cola.platform.tianquan.shoubing.resource-server-id=tianshu-local",
                        "egon.cola.platform.tianquan.shoubing.resource-uri=https://resource.example/tianshu"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(
                            IdentityResourceServerStateReader.class
                    );
                    assertThat(context).hasSingleBean(
                            DdcRegistrationCredentialVerifier.class
                    );
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestInfrastructure {

        @Bean("ddcAdminRedissonClient")
        RedissonClient ddcAdminRedissonClient() {
            return mock(RedissonClient.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        DdcInstanceRepository ddcInstanceRepository() {
            return mock(DdcInstanceRepository.class);
        }

        @Bean
        DdcScopeGate ddcScopeGate() {
            return mock(DdcScopeGate.class);
        }
    }
}
