package top.egon.cola.component.rpc.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.rpc.provider.server.RpcAccessGuardExceptionMapper;

import static org.assertj.core.api.Assertions.assertThat;

class RpcAccessGuardAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            RpcAccessGuardAutoConfiguration.class));

    @Test
    void providerEnabledInstallsOptionalGuardMapper() {
        contextRunner
                .withPropertyValues(
                        "egon.cola.component.rpc.provider.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(
                            RpcAccessGuardExceptionMapper.class);
                });
    }

    @Test
    void disabledOrConsumerOnlyDoesNotInstallProviderMapper() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(
                    RpcAccessGuardExceptionMapper.class);
        });
        contextRunner
                .withPropertyValues(
                        "egon.cola.component.rpc.provider.enabled=false",
                        "egon.cola.component.rpc.consumer.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(
                            RpcAccessGuardExceptionMapper.class);
                });
    }
}
