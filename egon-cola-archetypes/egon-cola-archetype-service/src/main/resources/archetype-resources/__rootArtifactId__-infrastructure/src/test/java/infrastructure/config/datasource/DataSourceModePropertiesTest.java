package ${package}.infrastructure.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class DataSourceModePropertiesTest {

    @Test
    void shouldDefaultToShardingAndBindEverySupportedMode() {
        assertThat(new DataSourceModeProperties(null).mode())
                .isEqualTo(DataSourceModeProperties.DataSourceMode.SHARDING);

        assertThat(DataSourceModeProperties.DataSourceMode.values())
                .containsExactly(
                        DataSourceModeProperties.DataSourceMode.SHARDING,
                        DataSourceModeProperties.DataSourceMode.SHARDING_READWRITE);

        for (DataSourceModeProperties.DataSourceMode mode
                : DataSourceModeProperties.DataSourceMode.values()) {
            DataSourceModeProperties properties = bind(mode.name());
            assertThat(properties.mode()).isEqualTo(mode);
        }
    }

    @Test
    void shouldRejectRemovedSingleAndUnknownModes() {
        assertThatThrownBy(() -> bind("SINGLE"))
                .hasMessageContaining("app.datasource.mode");
        assertThatThrownBy(() -> bind("READWRITE"))
                .hasMessageContaining("app.datasource.mode");
    }

    private static DataSourceModeProperties bind(String mode) {
        return new Binder(new MapConfigurationPropertySource(
                        Map.of("app.datasource.mode", mode)))
                .bind("app.datasource", Bindable.of(DataSourceModeProperties.class))
                .get();
    }
}
