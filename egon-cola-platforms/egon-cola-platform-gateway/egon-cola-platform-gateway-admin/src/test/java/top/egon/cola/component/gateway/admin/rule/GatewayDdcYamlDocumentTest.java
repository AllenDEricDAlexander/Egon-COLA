package top.egon.cola.component.gateway.admin.rule.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.environment.DdcDynamicPropertySource;
import top.egon.cola.component.ddc.format.DdcYamlConfigFormatStrategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayDdcYamlDocumentTest {

    private final GatewayDdcYamlDocument document =
            new GatewayDdcYamlDocument();

    @Test
    void updatesOneRuleLeafAndPreservesOtherBusinessConfiguration()
            throws Exception {
        String updated = document.putLeaf("""
                feature:
                  checkout: true
                gateway:
                  rules:
                    active: old
                """, GatewayDdcYamlDocument.ACTIVE_CONFIG_KEY, "new");

        DdcDynamicPropertySource source = loader().load(
                DdcYamlConfigFormatStrategy.DEFAULT_RESOURCE_NAME,
                updated,
                1L
        );
        assertThat(source.getProperty("feature.checkout")).isEqualTo(true);
        assertThat(source.getProperty("gateway.rules.active"))
                .isEqualTo("new");
    }

    @Test
    void supportsDottedSpringBootKeysAndRemovesOnlyTheRequestedChunk()
            throws Exception {
        String withChunk = document.putLeaf("""
                gateway.rules:
                  active: activation
                """, "gateway.rules.chunk.release-1.0", "chunk-0");

        top.egon.cola.component.gateway.admin.rule.service.GatewayYamlRemoval removal = document.removeLeaf(
                withChunk,
                "gateway.rules.chunk.release-1.0"
        );

        assertThat(removal.removed()).isTrue();
        assertThat(removal.content()).doesNotContain("release-1");
        DdcDynamicPropertySource source = loader().load(
                DdcYamlConfigFormatStrategy.DEFAULT_RESOURCE_NAME,
                removal.content(),
                2L
        );
        assertThat(source.getProperty("gateway.rules.active"))
                .isEqualTo("activation");
        assertThat(source.getProperty("gateway.rules.chunk.release-1.0"))
                .isNull();
    }

    @Test
    void readsFrozenLeafAndRejectsAmbiguousOrUnsupportedPaths() {
        String content = """
                gateway.rules.active: direct
                gateway:
                  rules:
                    active: nested
                """;

        assertThatThrownBy(() -> document.leafValue(
                content,
                GatewayDdcYamlDocument.ACTIVE_CONFIG_KEY
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ambiguous");
        assertThatThrownBy(() -> document.putLeaf(
                "feature: true\n",
                "feature.enabled",
                "true"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported");
    }

    private DdcYamlConfigFormatStrategy loader() {
        return new DdcYamlConfigFormatStrategy();
    }
}
