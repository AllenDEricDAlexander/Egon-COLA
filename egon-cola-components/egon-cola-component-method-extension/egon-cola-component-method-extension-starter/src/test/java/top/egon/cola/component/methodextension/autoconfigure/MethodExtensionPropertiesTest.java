package top.egon.cola.component.methodextension.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MethodExtensionPropertiesTest {

    @Test
    void defaultsToAopAndDisabledAlwaysWins() {
        MethodExtensionProperties properties = new MethodExtensionProperties();

        assertThat(properties.getEngine()).isEqualTo(MethodExtensionEngine.AOP);
        assertThat(properties.getNotReadyPolicy())
                .isEqualTo(MethodExtensionNotReadyPolicy.PROCEED);
        assertThat(properties.effectiveEngine()).isEqualTo(MethodExtensionEngine.AOP);

        properties.setEngine(MethodExtensionEngine.AGENT);
        assertThat(properties.effectiveEngine()).isEqualTo(MethodExtensionEngine.AGENT);
        properties.setEnabled(false);
        assertThat(properties.effectiveEngine()).isEqualTo(MethodExtensionEngine.DISABLED);
    }
}
