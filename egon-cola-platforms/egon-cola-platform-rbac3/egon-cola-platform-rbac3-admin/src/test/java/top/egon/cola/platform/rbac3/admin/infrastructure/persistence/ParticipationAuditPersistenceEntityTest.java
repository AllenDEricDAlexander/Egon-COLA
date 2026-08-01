package top.egon.cola.platform.rbac3.admin.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import top.egon.cola.platform.rbac3.admin.audit.domain.AuditLogEntity;
import top.egon.cola.platform.rbac3.admin.participation.domain.BusinessParticipationEntity;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ParticipationAuditPersistenceEntityTest {

    private static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");

    @Test
    void mapsAppendOnlyParticipationIdentityAndPayloadDigest() {
        BusinessParticipationEntity entity = new BusinessParticipationEntity(
                1L, 7L, "finance-service", "PAYMENT", "PAY-1", 9L,
                "SUBMIT", "event-1", NOW.minusSeconds(5), "trace-1",
                "sha256:payload", NOW, "finance-service");

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getTenantId()).isEqualTo(7L);
        assertThat(entity.getBusinessEventId()).isEqualTo("event-1");
        assertThat(entity.getPayloadDigest()).isEqualTo("sha256:payload");
        assertThat(entity.getOccurredAt()).isEqualTo(NOW.minusSeconds(5));
    }

    @Test
    void mapsOnlyRedactedAuditSnapshotsAndChecksum() {
        AuditLogEntity entity = new AuditLogEntity(
                2L, 7L, "ROLE_CHANGED", "SUCCESS", "HIGH", "USER", "user-1",
                "ROLE", "role-1", null, "ALLOW", "request-1", "trace-1",
                null, null, Map.of("password", "<redacted>"), Map.of("result", "ok"),
                "sha256:audit", NOW);

        assertThat(entity.getId()).isEqualTo(2L);
        assertThat(entity.getTenantId()).isEqualTo(7L);
        assertThat(entity.getBeforeSnapshot()).containsEntry("password", "<redacted>");
        assertThat(entity.getPayloadChecksum()).isEqualTo("sha256:audit");
        assertThat(entity.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void bindsAuditClientAddressesAsPostgresqlInetValues() throws Exception {
        JdbcTypeCode jdbcType = AuditLogEntity.class
                .getDeclaredField("clientIp")
                .getAnnotation(JdbcTypeCode.class);

        assertThat(jdbcType).isNotNull();
        assertThat(jdbcType.value()).isEqualTo(SqlTypes.INET);
    }
}
