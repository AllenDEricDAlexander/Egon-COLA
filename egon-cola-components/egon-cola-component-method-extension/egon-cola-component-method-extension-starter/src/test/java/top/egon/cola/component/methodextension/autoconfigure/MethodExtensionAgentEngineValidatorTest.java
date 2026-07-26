package top.egon.cola.component.methodextension.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.methodextension.exception.MethodExtensionConfigurationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MethodExtensionAgentEngineValidatorTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MethodExtensionAutoConfiguration.class));

    @Test
    void agentEngineWithoutTheAgentIntegrationFailsStartup() {
        contextRunner.withPropertyValues("egon.cola.component.method-extension.engine=AGENT")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .isInstanceOf(MethodExtensionConfigurationException.class)
                            .hasMessageContaining("engine=AOP");
                });
    }

    @Test
    void aopEngineStartsCleanly() {
        contextRunner.run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void disabledEngineSkipsValidation() {
        contextRunner.withPropertyValues(
                        "egon.cola.component.method-extension.engine=AGENT",
                        "egon.cola.component.method-extension.enabled=false")
                .run(context -> assertThat(context).hasNotFailed());
    }

    /**
     * The bytecode starter depends on this module, so its adapter cannot be referenced here; the
     * presence check is supplied instead.
     */
    @Test
    void agentEngineIsAcceptedWhenTheIntegrationIsPresent() {
        MethodExtensionProperties properties = new MethodExtensionProperties();
        properties.setEngine(MethodExtensionEngine.AGENT);

        assertThatThrownBy(() -> new MethodExtensionAgentEngineValidator(properties, () -> false)
                .afterPropertiesSet())
                .isInstanceOf(MethodExtensionConfigurationException.class);

        assertThatCode(() -> new MethodExtensionAgentEngineValidator(properties, () -> true)
                .afterPropertiesSet())
                .doesNotThrowAnyException();
    }
}
