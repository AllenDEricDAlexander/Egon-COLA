package top.egon.cola.component.ddc.management.client;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcManagementClientPropertiesTest {

    @Test
    void rejectsMissingCredentialsAndInvalidTimeouts() {
        assertThatThrownBy(() -> new DdcManagementClientProperties(
                "http://ddc.test",
                "ak",
                " ",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2)
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new DdcManagementClientProperties(
                "http://ddc.test",
                "ak",
                "sk",
                Duration.ZERO,
                Duration.ofSeconds(2)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void diagnosticTextRedactsTheSecret() {
        DdcManagementClientProperties properties = new DdcManagementClientProperties(
                "http://ddc.test/",
                "ak",
                "do-not-leak",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2)
        );

        assertThat(properties.endpoint()).isEqualTo("http://ddc.test");
        assertThat(properties.toString())
                .contains("secretKey=******")
                .doesNotContain("do-not-leak");
    }
}
