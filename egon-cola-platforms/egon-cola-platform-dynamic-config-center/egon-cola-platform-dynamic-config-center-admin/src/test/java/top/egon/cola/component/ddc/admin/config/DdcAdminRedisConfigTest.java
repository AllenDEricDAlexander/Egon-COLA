package top.egon.cola.component.ddc.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.ddc.admin.repository.DdcInstanceRepository;
import top.egon.cola.component.ddc.admin.security.registration.DdcRegistrationCredentialVerifier;
import top.egon.cola.component.ddc.admin.service.metadata.DdcScopeGate;
import top.egon.cola.platform.idp.starter.autoconfigure.IdpStarterAutoConfiguration;
import top.egon.cola.platform.idp.starter.security.ServiceAccessTokenVerifier;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerStateReader;

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
                        "egon.cola.platform.idp.enabled=true",
                        "egon.cola.platform.idp.issuer=https://idp.example",
                        "egon.cola.platform.idp.jwk-set-uri=https://idp.example/oauth2/jwks",
                        "egon.cola.platform.idp.resource-server-id=ddc-local",
                        "egon.cola.platform.idp.resource-uri=https://resource.example/ddc"
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

        @Bean
        ServiceAccessTokenVerifier serviceAccessTokenVerifier() {
            return mock(ServiceAccessTokenVerifier.class);
        }
    }
}
