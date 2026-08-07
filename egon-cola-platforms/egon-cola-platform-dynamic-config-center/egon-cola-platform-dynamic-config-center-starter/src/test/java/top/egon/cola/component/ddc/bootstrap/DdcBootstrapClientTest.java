package top.egon.cola.component.ddc.bootstrap;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcBootstrapClientTest {

    @Test
    void pullsScopeOnlyOnce() {
        AtomicInteger pulls = new AtomicInteger();
        DdcConfigValue value = value(
                "application.yml",
                "YAML",
                "feature:\n  enabled: true\n",
                3L
        );
        DdcBootstrapClient client = new DdcBootstrapClient(() -> {
            pulls.incrementAndGet();
            return List.of(value);
        }, 1024);

        assertThat(client.load("application.yml")).isSameAs(value);
        assertThat(client.load("application.yml")).isSameAs(value);
        assertThat(pulls).hasValue(1);
    }

    @Test
    void emptyScopeRepresentsMissingResource() {
        DdcBootstrapClient client = new DdcBootstrapClient(
                List::<DdcConfigValue>of,
                1024
        );

        assertThat(client.load("application.yml")).isNull();
    }

    @Test
    void rejectsLegacyOrAmbiguousResources() {
        DdcBootstrapClient multiple = new DdcBootstrapClient(
                () -> List.of(
                        value("application.yml", "YAML", "a: 1", 1L),
                        value("feature.enabled", "BOOLEAN", "true", 1L)
                ),
                1024
        );
        DdcBootstrapClient wrongType = new DdcBootstrapClient(
                () -> List.of(value(
                        "application.yml",
                        "STRING",
                        "a: 1",
                        1L
                )),
                1024
        );

        assertThatThrownBy(() -> multiple.load("application.yml"))
                .hasMessage(
                        "DDC scope must contain exactly one application.yml"
                );
        assertThatThrownBy(() -> wrongType.load("application.yml"))
                .hasMessage(
                        "DDC scope must contain only application.yml with YAML type"
                );
    }

    @Test
    void rejectsInvalidVersionAndOversizedUtf8Content() {
        DdcBootstrapClient invalidVersion = new DdcBootstrapClient(
                () -> List.of(value(
                        "application.yml",
                        "YAML",
                        "a: 1",
                        0L
                )),
                1024
        );
        DdcBootstrapClient oversized = new DdcBootstrapClient(
                () -> List.of(value(
                        "application.yml",
                        "YAML",
                        "a: 你好",
                        1L
                )),
                5
        );

        assertThatThrownBy(() -> invalidVersion.load("application.yml"))
                .hasMessage("DDC application.yml must have a positive version");
        assertThatThrownBy(() -> oversized.load("application.yml"))
                .hasMessage(
                        "DDC application.yml exceeds the UTF-8 limit of 5 bytes"
                );
    }

    private DdcConfigValue value(String key,
                                 String type,
                                 String content,
                                 Long version) {
        DdcConfigValue value = new DdcConfigValue();
        value.setConfigKey(key);
        value.setValueType(type);
        value.setConfigValue(content);
        value.setVersion(version);
        return value;
    }
}
