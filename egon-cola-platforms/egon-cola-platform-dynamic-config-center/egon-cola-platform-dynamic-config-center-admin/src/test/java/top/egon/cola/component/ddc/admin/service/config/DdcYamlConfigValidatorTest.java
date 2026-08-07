package top.egon.cola.component.ddc.admin.service.config;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcYamlConfigValidatorTest {

    private final DdcYamlConfigValidator validator =
            new DdcYamlConfigValidator(64);

    @Test
    void acceptsSingleMapRootDocumentWithinUtf8Limit() {
        assertThatCode(() -> validator.validate(
                "feature:\n  enabled: true\n"
        )).doesNotThrowAnyException();
    }

    @Test
    void rejectsMalformedEmptyMultipleAndNonMapDocuments() {
        assertInvalid("feature: [", "invalid application.yml");
        assertInvalid("", "invalid application.yml");
        assertInvalid("feature: true\n---\nother: true\n", "invalid application.yml");
        assertInvalid("value", "invalid application.yml");
        assertInvalid("- first\n- second\n", "invalid application.yml");
    }

    @Test
    void rejectsDdcConfigImportAndProfileSelectionKeys() {
        assertInvalid(
                "egon:\n  cola:\n    component:\n      ddc:\n        enabled: false\n",
                "reserved"
        );
        assertInvalid(
                "spring:\n  config:\n    import: optional:file:local.yml\n",
                "reserved"
        );
        assertInvalid(
                "spring:\n  profiles:\n    active: prod\n",
                "reserved"
        );
    }

    @Test
    void measuresUtf8BytesWithoutEchoingRejectedContent() {
        DdcYamlConfigValidator smallValidator =
                new DdcYamlConfigValidator(8);

        assertThatThrownBy(() -> smallValidator.validate("name: 你好\n"))
                .isInstanceOf(DdcAdminException.class)
                .hasMessageContaining("8")
                .hasMessageNotContaining("你好");
    }

    @Test
    void rejectsNonPositiveCapacity() {
        assertThatThrownBy(() -> new DdcYamlConfigValidator(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    private void assertInvalid(String content, String message) {
        assertThatThrownBy(() -> validator.validate(content))
                .isInstanceOf(DdcAdminException.class)
                .hasMessageContaining(message);
    }
}
