package top.egon.cola.platform.idp.gateway.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.gateway.core.security.GatewayAuthenticationProvider;
import top.egon.cola.component.gateway.core.security.GatewayAuthorizationProvider;
import top.egon.cola.component.gateway.core.security.GatewayCredentialExtractor;
import top.egon.cola.component.gateway.core.security.GatewayIdentityMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class IdpGatewayAdapterAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    IdpGatewayAdapterAutoConfiguration.class))
            .withBean(ObjectMapper.class,
                    () -> new ObjectMapper().findAndRegisterModules());

    @Test
    void staysDisabledByDefault() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(GatewayCredentialExtractor.class);
            assertThat(context).doesNotHaveBean(GatewayIdentityMapper.class);
        });
    }

    @Test
    void registersOnlyIdentityCapabilitiesWhenRedisIsAvailable() {
        runner.withPropertyValues(
                        "egon.cola.platform.idp.gateway.enabled=true",
                        "egon.cola.platform.idp.gateway.issuer=https://idp.local",
                        "egon.cola.platform.idp.gateway.jwk-set-uri=https://idp.local/oauth2/jwks",
                        "egon.cola.platform.idp.gateway.audiences[0]=egon-api",
                        "egon.cola.platform.idp.gateway.client-ids[0]=gateway-client",
                        "egon.cola.platform.idp.gateway.runtime.redis-enabled=false")
                .withBean("idpGatewayRedissonClient", RedissonClient.class,
                        () -> mock(RedissonClient.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(GatewayCredentialExtractor.class);
                    assertThat(context).hasSingleBean(GatewayAuthenticationProvider.class);
                    assertThat(context).hasSingleBean(GatewayIdentityMapper.class);
                    assertThat(context).doesNotHaveBean(
                            GatewayAuthorizationProvider.class);
                    assertThat(context.getBean(GatewayCredentialExtractor.class)
                            .extractorId()).isEqualTo("idp-bearer");
                    assertThat(context.getBean(GatewayAuthenticationProvider.class)
                            .providerId()).isEqualTo("idp-jwt");
                    assertThat(context.getBean(GatewayIdentityMapper.class)
                            .mapperId()).isEqualTo("idp-identity");
                });
    }
}
