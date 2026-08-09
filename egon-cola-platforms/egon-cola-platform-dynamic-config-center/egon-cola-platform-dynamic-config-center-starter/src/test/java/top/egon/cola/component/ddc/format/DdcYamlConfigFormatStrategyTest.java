package top.egon.cola.component.ddc.format;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.configuration.environment.DdcDynamicPropertySource;
import top.egon.cola.component.ddc.configuration.environment.DdcReservedConfigurationKeys;
import top.egon.cola.component.ddc.model.config.DdcConfigFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcYamlConfigFormatStrategyTest {

    private final DdcYamlConfigFormatStrategy strategy =
            new DdcYamlConfigFormatStrategy();

    @Test
    void declaresYamlFormatAndSupportedResourceNames() {
        assertThat(strategy.format()).isEqualTo(DdcConfigFormat.YAML);
        assertThat(strategy.supports("application.yml")).isTrue();
        assertThat(strategy.supports("application.yaml")).isTrue();
        assertThat(strategy.supports("application.json")).isFalse();
    }

    @Test
    void delegatesFlatteningAndOriginTrackingToBootLoader() throws Exception {
        DdcDynamicPropertySource source = strategy.load(
                "application.yml",
                "feature:\n  enabled: true\n  names:\n    - first\n",
                7L
        );

        assertThat(source.getName()).isEqualTo("ddc:application.yml");
        assertThat(source.getProperty("feature.enabled")).isEqualTo(true);
        assertThat(source.getProperty("feature.names[0]")).isEqualTo("first");
        assertThat(source.getOrigin("feature.enabled")).isNotNull();
        assertThat(source.snapshot().resourceName()).isEqualTo("application.yml");
        assertThat(source.snapshot().version()).isEqualTo(7L);
        assertThat(source.snapshot().values())
                .containsEntry("feature.enabled", true)
                .containsEntry("feature.names[0]", "first");
        assertThat(source.isImmutable()).isFalse();
    }

    @Test
    void replacesTheCompleteSnapshotWithoutChangingSourcePosition()
            throws Exception {
        DdcDynamicPropertySource source = strategy.load(
                "application.yml",
                "feature:\n  enabled: true\n  old-value: local\n",
                1L
        );
        DdcDynamicPropertySource replacement = strategy.load(
                "application.yml",
                "feature:\n  enabled: false\n  new-value: remote\n",
                2L
        );

        source.replace(replacement.snapshot());

        assertThat(source.getName()).isEqualTo("ddc:application.yml");
        assertThat(source.getProperty("feature.enabled")).isEqualTo(false);
        assertThat(source.getProperty("feature.old-value")).isNull();
        assertThat(source.getProperty("feature.new-value")).isEqualTo("remote");
        assertThat(source.snapshot().version()).isEqualTo(2L);
    }

    @Test
    void rejectsEmptyMultipleAndNonMappingDocuments() {
        assertThatThrownBy(() -> strategy.load(
                "application.yml",
                "  \n",
                1L
        )).hasMessage("DDC remote YAML must not be empty");
        assertThatThrownBy(() -> strategy.load(
                "application.yml",
                "one: 1\n---\ntwo: 2\n",
                1L
        )).hasMessage("DDC remote YAML must contain exactly one document");
        assertThatThrownBy(() -> strategy.load(
                "application.yml",
                "- one\n- two\n",
                1L
        )).hasMessageContaining("mapping");
        assertThatThrownBy(() -> strategy.load(
                "application.yml",
                "plain text\n",
                1L
        )).hasMessageContaining("mapping");
    }

    @Test
    void rejectsDdcConfigImportsAndProfileSelectionAsWholeDocument() {
        for (String content : new String[]{
                "egon:\n  cola:\n    component:\n      ddc:\n        enabled: false\n",
                "spring:\n  config:\n    import: classpath:other.yml\n",
                "spring:\n  profiles:\n    active: prod\n",
                "SPRING_CONFIG_IMPORT: classpath:other.yml\n"
        }) {
            assertThatThrownBy(() -> strategy.load(
                    "application.yml",
                    content,
                    1L
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reserved key");
        }
    }

    @Test
    void reservedKeyMatchingIncludesDescendantsAndRelaxedUnderscores() {
        assertThat(DdcReservedConfigurationKeys.isReserved(
                "egon.cola.component.ddc.admin.endpoint"
        )).isTrue();
        assertThat(DdcReservedConfigurationKeys.isReserved(
                "spring.profiles.group.production[0]"
        )).isTrue();
        assertThat(DdcReservedConfigurationKeys.isReserved(
                "SPRING_CONFIG_ADDITIONAL_LOCATION"
        )).isTrue();
        assertThat(DdcReservedConfigurationKeys.isReserved(
                "business.feature.enabled"
        )).isFalse();
    }
}
