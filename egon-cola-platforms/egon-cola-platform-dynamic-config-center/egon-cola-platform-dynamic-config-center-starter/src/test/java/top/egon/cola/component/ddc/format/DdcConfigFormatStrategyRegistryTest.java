package top.egon.cola.component.ddc.format;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.config.DdcConfigFormat;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcConfigFormatStrategyRegistryTest {

    @Test
    void defaultRegistryContainsOnlyYamlStrategy() {
        DdcConfigFormatStrategyRegistry registry =
                DdcConfigFormatStrategyRegistry.defaults();

        assertThat(registry.get(DdcConfigFormat.YAML))
                .isInstanceOf(DdcYamlConfigFormatStrategy.class);
        assertThat(registry.get("yaml", "application.yml"))
                .isSameAs(registry.get(DdcConfigFormat.YAML));
        assertThat(registry.getByResourceName("application.yaml"))
                .isSameAs(registry.get(DdcConfigFormat.YAML));
    }

    @Test
    void rejectsUnknownFormatsAndMismatchedResourceNames() {
        DdcConfigFormatStrategyRegistry registry =
                DdcConfigFormatStrategyRegistry.defaults();

        assertThatThrownBy(() -> registry.get("json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported DDC config format");
        assertThatThrownBy(() -> registry.get("yaml", "application.json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match");
        assertThatThrownBy(() -> registry.getByResourceName("application.properties"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported DDC config resource");
    }

    @Test
    void rejectsDuplicateStrategiesForOneFormat() {
        assertThatThrownBy(() -> new DdcConfigFormatStrategyRegistry(List.of(
                new DdcYamlConfigFormatStrategy(),
                new DdcYamlConfigFormatStrategy()
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate");
    }
}
