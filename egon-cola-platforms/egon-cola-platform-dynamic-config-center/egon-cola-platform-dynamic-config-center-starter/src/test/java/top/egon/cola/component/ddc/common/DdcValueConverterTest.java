package top.egon.cola.component.ddc.common;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcValueConverterTest {

    enum Mode {
        ON, OFF
    }

    static class JsonConfig {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private final DdcValueConverter converter = new DdcValueConverter();

    @Test
    void convertsScalarTypes() {
        assertThat(converter.convert("demo", String.class)).isEqualTo("demo");
        assertThat(converter.convert("1", Integer.class)).isEqualTo(1);
        assertThat(converter.convert("2", int.class)).isEqualTo(2);
        assertThat(converter.convert("3", Long.class)).isEqualTo(3L);
        assertThat(converter.convert("true", Boolean.class)).isEqualTo(true);
        assertThat(converter.convert("1.5", Double.class)).isEqualTo(1.5D);
        assertThat(converter.convert("9.99", BigDecimal.class)).isEqualByComparingTo("9.99");
    }

    @Test
    void convertsEnumListAndJsonObject() {
        assertThat(converter.convert("ON", Mode.class)).isEqualTo(Mode.ON);
        assertThat(converter.convert("[\"a\",\"b\"]", List.class)).containsExactly("a", "b");
        JsonConfig config = converter.convert("{\"name\":\"demo\"}", JsonConfig.class);
        assertThat(config.getName()).isEqualTo("demo");
    }

    @Test
    void wrapsConversionFailure() {
        assertThatThrownBy(() -> converter.convert("bad", Integer.class))
                .isInstanceOf(DdcException.class)
                .hasMessageContaining("convert config value failed");
    }
}
