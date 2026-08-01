package top.egon.cola.platform.rbac3.admin.audit;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.audit.application.AuditQueryService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditRedactionIT {

    private static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");

    @Test
    void redactsSecretsRecursivelyBeforeAppendAndAuditsTheRead() {
        InMemoryStore store = new InMemoryStore();
        AuditQueryService service = new AuditQueryService(
                store, Clock.fixed(NOW, ZoneOffset.UTC));
        service.record(new AuditQueryService.AuditCommand(
                "tenant-1", "ROLE_CHANGED", "SUCCESS", "HIGH", "USER", "user-1",
                "ROLE", "role-1", null, "ALLOW", "request-1", "trace-1",
                Map.of("password", "plaintext", "profile", Map.of("name", "Mario")),
                Map.of("authorization", "Bearer secret", "result", "ok"), NOW));

        AuditQueryService.AuditView stored = store.appended.getFirst();
        assertThat(stored.beforeSnapshot())
                .containsEntry("password", "<redacted>")
                .containsEntry("profile", Map.of("name", "Mario"));
        assertThat(stored.afterSnapshot())
                .containsEntry("authorization", "<redacted>")
                .containsEntry("result", "ok");
        assertThat(stored.payloadChecksum()).startsWith("sha256:");

        AuditQueryService.Page page = service.query(new AuditQueryService.Query(
                "tenant-1", NOW.minusSeconds(3600), NOW, null, null,
                "ROLE_CHANGED", "SUCCESS", null, null, null, null, 50, null),
                "auditor-1", "audit-request", "audit-trace");

        assertThat(page.items()).hasSize(1);
        assertThat(store.appended).extracting(AuditQueryService.AuditView::eventType)
                .containsExactly("ROLE_CHANGED", "AUDIT_LOGS_READ");
    }

    @Test
    void rejectsUnboundedAuditWindowAndMalformedPageSize() {
        AuditQueryService service = new AuditQueryService(
                new InMemoryStore(), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.query(new AuditQueryService.Query(
                        "tenant-1", NOW.minusSeconds(32L * 24 * 3600), NOW,
                        null, null, null, null, null, null, null, null, 50, null),
                "auditor-1", "request-1", "trace-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("31 days");
        assertThatThrownBy(() -> service.query(new AuditQueryService.Query(
                        "tenant-1", NOW.minusSeconds(3600), NOW,
                        null, null, null, null, null, null, null, null, 201, null),
                "auditor-1", "request-1", "trace-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pageSize");
    }

    private static final class InMemoryStore implements AuditQueryService.AuditStore {
        private final List<AuditQueryService.AuditView> appended = new ArrayList<>();

        @Override
        public AuditQueryService.AuditView append(AuditQueryService.AuditView record) {
            appended.add(record);
            return record;
        }

        @Override
        public AuditQueryService.Page query(AuditQueryService.Query query) {
            return new AuditQueryService.Page(
                    appended.stream()
                            .filter(record -> !record.eventType().equals("AUDIT_LOGS_READ"))
                            .toList(), null);
        }
    }
}
