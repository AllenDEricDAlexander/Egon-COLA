package top.egon.cola.component.common.mybatis.ddl;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlManifestBO.ScriptBO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EgonColaDdlManifestFamilyTest {

    private static final List<String> EXISTING_FAMILIES = List.of(
            "light",
            "light-open",
            "service",
            "service-open",
            "web",
            "web-open"
    );

    private static final ScriptBO COMPONENT_OUTBOX_SCRIPT = new ScriptBO(
            "20260924_001",
            "db/egon-outbox-mp/V20260924_001__initialize_outbox_mp_schema.sql",
            "a".repeat(64)
    );

    @Test
    void acceptsTheExactComponentOutboxFamily() {
        EgonColaDdlManifestBO manifest = new EgonColaDdlManifestBO(
                "component-outbox",
                List.of(COMPONENT_OUTBOX_SCRIPT)
        );

        assertThat(manifest.family()).isEqualTo("component-outbox");
        assertThat(manifest.scripts()).containsExactly(COMPONENT_OUTBOX_SCRIPT);
    }

    @Test
    void keepsEveryExistingArchetypeFamilyAccepted() {
        for (String family : EXISTING_FAMILIES) {
            assertThat(new EgonColaDdlManifestBO(family, List.of(COMPONENT_OUTBOX_SCRIPT)).family())
                    .isEqualTo(family);
        }
    }

    @Test
    void continuesRejectingEveryUnregisteredFamily() {
        assertThatThrownBy(() -> new EgonColaDdlManifestBO(
                "arbitrary",
                List.of(COMPONENT_OUTBOX_SCRIPT)
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
