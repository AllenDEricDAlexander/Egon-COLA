package top.egon.cola.component.common.mybatis.ddl;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlManifestBO.ScriptBO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EgonColaTianshuDdlManifestTest {

    private static final List<String> EXISTING_FAMILIES = List.of(
            "light", "light-open", "service", "service-open", "web", "web-open", "component-outbox");
    private static final ScriptBO TIANSHU_SCRIPT = new ScriptBO(
            "20260922_001", "db/egon-mp/V20260922_001__tianshu_platform.sql", "a".repeat(64));

    @Test
    void acceptsTianshuWithoutReplacingExistingFamilies() {
        EgonColaDdlManifestBO manifest = new EgonColaDdlManifestBO("tianshu", List.of(TIANSHU_SCRIPT));

        assertThat(manifest.family()).isEqualTo("tianshu");
        assertThat(manifest.scripts()).containsExactly(TIANSHU_SCRIPT);
    }

    @Test
    void keepsAllPreviouslyRegisteredFamiliesAccepted() {
        for (String family : EXISTING_FAMILIES) {
            ScriptBO script = new ScriptBO(
                    "20260922_001", "db/test/V20260922_001__manifest_test.sql", "b".repeat(64));
            assertThat(new EgonColaDdlManifestBO(family, List.of(script)).family()).isEqualTo(family);
        }
    }

    @Test
    void unsupportedFamiliesRemainRejected() {
        assertThatThrownBy(() -> new EgonColaDdlManifestBO("arbitrary", List.of(TIANSHU_SCRIPT)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void preservesVersionPathAndDigestValidation() {
        assertThatThrownBy(() -> new EgonColaDdlManifestBO("tianshu", List.of(
                new ScriptBO("20260922_002", "db/egon-mp/V20260922_002__second.sql", "c".repeat(64)),
                TIANSHU_SCRIPT))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ScriptBO("20260922_001", "../outside.sql", "a".repeat(64)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ScriptBO("20260922_001", TIANSHU_SCRIPT.path(), "bad-digest"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
