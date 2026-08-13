package top.egon.cola.platform.rbac3.admin.audit;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.audit.repository.internal.AuditCursorCodec;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import top.egon.cola.platform.rbac3.admin.audit.domain.vo.CursorPositionVO;

class AuditCursorCodecTest {

    private final AuditCursorCodec codec = new AuditCursorCodec(
            "test-only-cursor-signing-key-with-32-bytes".getBytes(StandardCharsets.UTF_8));

    @Test
    void roundTripsPositionOnlyForTheSameTenantAndFilter() {
        var position = new CursorPositionVO(
                Instant.parse("2026-07-30T12:00:00Z"), 901L);

        String cursor = codec.encode(position, "tenant-1", "filter-a");

        assertThat(codec.decode(cursor, "tenant-1", "filter-a")).isEqualTo(position);
        assertThatThrownBy(() -> codec.decode(cursor, "tenant-2", "filter-a"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cursor");
        assertThatThrownBy(() -> codec.decode(cursor, "tenant-1", "filter-b"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cursor");
    }

    @Test
    void rejectsTamperingAndMalformedCursor() {
        String cursor = codec.encode(
                new CursorPositionVO(
                        Instant.parse("2026-07-30T12:00:00Z"), 901L),
                "tenant-1", "filter-a");
        String tampered = cursor.substring(0, cursor.length() - 1)
                + (cursor.endsWith("A") ? "B" : "A");

        assertThatThrownBy(() -> codec.decode(tampered, "tenant-1", "filter-a"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cursor");
        assertThatThrownBy(() -> codec.decode("not-a-cursor", "tenant-1", "filter-a"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cursor");
    }
}
