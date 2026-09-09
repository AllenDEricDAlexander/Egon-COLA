package top.egon.cola.component.yuheng.admin.rule.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.tianshu.environment.DdcDynamicPropertySource;
import top.egon.cola.component.tianshu.format.DdcYamlConfigFormatStrategy;

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
                yuheng:
                  rules:
                    active: old
                """, GatewayDdcYamlDocument.ACTIVE_CONFIG_KEY, "new");

        DdcDynamicPropertySource source = loader().load(
                DdcYamlConfigFormatStrategy.DEFAULT_RESOURCE_NAME,
                updated,
                1L
        );
        assertThat(source.getProperty("feature.checkout")).isEqualTo(true);
        assertThat(source.getProperty("yuheng.rules.active"))
                .isEqualTo("new");
    }

    @Test
    void supportsDottedSpringBootKeysAndRemovesOnlyTheRequestedChunk()
            throws Exception {
        String withChunk = document.putLeaf("""
                yuheng.rules:
                  active: activation
                """, "yuheng.rules.chunk.release-1.0", "chunk-0");

        top.egon.cola.component.yuheng.admin.rule.service.GatewayYamlRemoval removal = document.removeLeaf(
                withChunk,
                "yuheng.rules.chunk.release-1.0"
        );

        assertThat(removal.removed()).isTrue();
        assertThat(removal.content()).doesNotContain("release-1");
        DdcDynamicPropertySource source = loader().load(
                DdcYamlConfigFormatStrategy.DEFAULT_RESOURCE_NAME,
                removal.content(),
                2L
        );
        assertThat(source.getProperty("yuheng.rules.active"))
                .isEqualTo("activation");
        assertThat(source.getProperty("yuheng.rules.chunk.release-1.0"))
                .isNull();
    }

    @Test
    void readsFrozenLeafAndRejectsAmbiguousOrUnsupportedPaths() {
        String content = """
                yuheng.rules.active: direct
                yuheng:
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

    @Test
    void readsGatewayYamlDocumentsLargerThanSnakeYamlDefaultLimit() {
        String largeValue = "x".repeat(3_100_000);
        String content = GatewayDdcYamlDocument.ACTIVE_CONFIG_KEY
                + ": " + largeValue + "\n";

        assertThat(document.leafValue(
                content,
                GatewayDdcYamlDocument.ACTIVE_CONFIG_KEY
        )).hasValue(largeValue);
    }

    private DdcYamlConfigFormatStrategy loader() {
        return new DdcYamlConfigFormatStrategy();
    }
}
