package top.egon.cola.component.ddc.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcValueParserTest {

    @Test
    void parsesExpressionWithDefaultValue() {
        DdcValueDefinition definition = DdcValueParser.parse("downgradeSwitch:0", "", "", Integer.class);

        assertThat(definition.getKey()).isEqualTo("downgradeSwitch");
        assertThat(definition.getDefaultValue()).isEqualTo("0");
        assertThat(definition.getType()).isEqualTo(Integer.class);
    }

    @Test
    void explicitKeyAndDefaultOverrideExpression() {
        DdcValueDefinition definition = DdcValueParser.parse("ignored:1", "realKey", "2", String.class);

        assertThat(definition.getKey()).isEqualTo("realKey");
        assertThat(definition.getDefaultValue()).isEqualTo("2");
    }

    @Test
    void rejectsBlankKey() {
        assertThatThrownBy(() -> DdcValueParser.parse(":1", "", "", String.class))
                .isInstanceOf(DdcException.class)
                .hasMessageContaining("config key must not be blank");
    }
}
