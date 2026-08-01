package top.egon.cola.platform.rbac3.gateway.autoconfigure;

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

class Rbac3GatewayAdapterAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    Rbac3GatewayAdapterAutoConfiguration.class))
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
    void registersAllFourStableGatewayCapabilitiesWhenRuntimeIsAvailable() {
        runner.withPropertyValues(
                        "egon.cola.platform.rbac3.gateway.enabled=true",
                        "egon.cola.platform.rbac3.gateway.issuer=https://issuer.example",
                        "egon.cola.platform.rbac3.gateway.audience=orders")
                .withBean("rbac3RuntimeRedissonClient", RedissonClient.class,
                        () -> mock(RedissonClient.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(GatewayCredentialExtractor.class);
                    assertThat(context).hasSingleBean(GatewayAuthenticationProvider.class);
                    assertThat(context).hasSingleBean(GatewayAuthorizationProvider.class);
                    assertThat(context).hasSingleBean(GatewayIdentityMapper.class);
                    assertThat(context.getBean(GatewayCredentialExtractor.class)
                            .extractorId()).isEqualTo("rbac3-bearer");
                    assertThat(context.getBean(GatewayAuthenticationProvider.class)
                            .providerId()).isEqualTo("rbac3-jwt-session");
                    assertThat(context.getBean(GatewayAuthorizationProvider.class)
                            .providerId()).isEqualTo("rbac3-permission");
                    assertThat(context.getBean(GatewayIdentityMapper.class)
                            .mapperId()).isEqualTo("rbac3-trusted-identity");
                });
    }
}
