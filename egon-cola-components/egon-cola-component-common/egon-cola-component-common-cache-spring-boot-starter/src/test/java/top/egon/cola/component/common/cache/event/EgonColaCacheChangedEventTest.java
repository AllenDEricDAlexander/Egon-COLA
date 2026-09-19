package top.egon.cola.component.common.cache.event;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.cache.event.EgonColaCacheChangedEvent.KeyGuard;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static top.egon.cola.component.common.cache.event.EgonColaCacheChangedOperation.EVICT;
import static top.egon.cola.component.common.cache.event.EgonColaCacheChangedOperation.PREFIX_EVICT;
import static top.egon.cola.component.common.cache.event.EgonColaCacheChangedOperation.PUT;

class EgonColaCacheChangedEventTest {

    private static final Instant NOW = Instant.parse("2026-09-18T00:00:00Z");

    private EgonColaCacheChangedEvent event(EgonColaCacheChangedOperation operation, List<String> keys) {
        return event(EgonColaCacheChangedEvent.SCHEMA_VERSION, operation, keys);
    }

    private EgonColaCacheChangedEvent event(int schemaVersion, EgonColaCacheChangedOperation operation,
                                            List<String> keys) {
        return new EgonColaCacheChangedEvent(schemaVersion, "evt-1", "node-1", NOW, "UserBO", operation, keys);
    }

    @Test
    void acceptsLegalExactAndGlobSamples() {
        EgonColaCacheChangedEvent evict = event(EVICT, List.of("41:7", "41:8"));
        assertThat(evict.schemaVersion()).isEqualTo(EgonColaCacheChangedEvent.SCHEMA_VERSION);
        assertThat(evict.eventId()).isEqualTo("evt-1");
        assertThat(evict.originNodeId()).isEqualTo("node-1");
        assertThat(evict.occurredAt()).isEqualTo(NOW);
        assertThat(evict.cacheName()).isEqualTo("UserBO");
        assertThat(evict.keys()).containsExactly("41:7", "41:8");

        assertThat(event(PREFIX_EVICT, List.of("41:*")).keys()).containsExactly("41:*");
        assertThat(event(PUT, List.of("41:7")).operation()).isEqualTo(PUT);
    }

    @Test
    void rejectsForeignTenantExactKeys() {
        assertThatThrownBy(() -> event(EVICT, List.of("41:7", "42:8")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CACHE_KEY_TENANT_MISMATCH");
    }

    @Test
    void rejectsBareKeyWithoutTenantSegment() {
        assertThatThrownBy(() -> event(EVICT, List.of("7")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CACHE_KEY_TENANT_MISMATCH");
    }

    @Test
    void rejectsIllegalGlobShapes() {
        for (String bad : List.of("*", ":*", "41:7:*", "*:8")) {
            assertThatThrownBy(() -> event(PREFIX_EVICT, List.of(bad)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CACHE_GLOB_PATTERN_FORBIDDEN");
        }
    }

    @Test
    void rejectsGlobUnderExactOperations() {
        assertThatThrownBy(() -> event(EVICT, List.of("41:*")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CACHE_GLOB_PATTERN_FORBIDDEN");
        assertThatThrownBy(() -> event(PUT, List.of("41:*")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CACHE_GLOB_PATTERN_FORBIDDEN");
    }

    @Test
    void rejectsExactKeyUnderPrefixEvict() {
        assertThatThrownBy(() -> event(PREFIX_EVICT, List.of("41:7")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CACHE_GLOB_PATTERN_FORBIDDEN");
    }

    @Test
    void rejectsUnsupportedSchemaVersion() {
        assertThatThrownBy(() -> event(2, EVICT, List.of("41:7")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CACHE_EVENT_UNSUPPORTED_SCHEMA");
    }

    @Test
    void rejectsBlankIdentityAndNullMembers() {
        assertThatThrownBy(() -> new EgonColaCacheChangedEvent(
                EgonColaCacheChangedEvent.SCHEMA_VERSION, " ", "node-1", NOW, "UserBO", EVICT, List.of("41:7")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EgonColaCacheChangedEvent(
                EgonColaCacheChangedEvent.SCHEMA_VERSION, "evt-1", "", NOW, "UserBO", EVICT, List.of("41:7")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EgonColaCacheChangedEvent(
                EgonColaCacheChangedEvent.SCHEMA_VERSION, "evt-1", "node-1", null, "UserBO", EVICT, List.of("41:7")))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new EgonColaCacheChangedEvent(
                EgonColaCacheChangedEvent.SCHEMA_VERSION, "evt-1", "node-1", NOW, "UserBO", null, List.of("41:7")))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsEmptyKeys() {
        assertThatThrownBy(() -> event(EVICT, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidCacheNames() {
        List<String> badNames = List.of("", " ", "bad name", "a:b", "a".repeat(101));
        for (String name : badNames) {
            assertThatThrownBy(() -> new EgonColaCacheChangedEvent(
                    EgonColaCacheChangedEvent.SCHEMA_VERSION, "evt-1", "node-1", NOW, name, EVICT, List.of("41:7")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CACHE_NAME_INVALID");
        }
        assertThatCode(() -> new EgonColaCacheChangedEvent(
                EgonColaCacheChangedEvent.SCHEMA_VERSION, "evt-1", "node-1", NOW,
                "top.egon_User-1", EVICT, List.of("41:7")))
                .doesNotThrowAnyException();
    }

    @Test
    void keysAreImmutableDefensiveCopy() {
        List<String> source = new ArrayList<>();
        source.add("41:7");
        EgonColaCacheChangedEvent event = event(EVICT, source);

        source.add("41:8");

        assertThat(event.keys()).containsExactly("41:7");
        assertThatThrownBy(() -> event.keys().add("41:9"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void keyGuardExposesTenantResolutionAndCurrentTenantMatch() {
        assertThat(KeyGuard.tenantOf("41:7")).isEqualTo(41L);
        assertThatCode(() -> KeyGuard.requireTenant("41:7", 41L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> KeyGuard.requireTenant("41:7", 42L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CACHE_KEY_TENANT_MISMATCH");
        assertThatCode(() -> KeyGuard.requireValidName("UserBO")).doesNotThrowAnyException();
        assertThatCode(() -> KeyGuard.requireExactKey("41:7")).doesNotThrowAnyException();
        assertThatCode(() -> KeyGuard.requireGlob("41:*")).doesNotThrowAnyException();
    }
}
