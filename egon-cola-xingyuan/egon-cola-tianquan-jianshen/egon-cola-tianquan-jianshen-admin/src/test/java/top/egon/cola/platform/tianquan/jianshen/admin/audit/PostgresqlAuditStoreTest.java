package top.egon.cola.platform.tianquan.jianshen.admin.audit;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.platform.tianquan.jianshen.admin.audit.domain.po.AuditLogPO;
import top.egon.cola.platform.tianquan.jianshen.admin.audit.domain.vo.AuditVO;
import top.egon.cola.platform.tianquan.jianshen.admin.audit.repository.internal.AuditCursorCodec;
import top.egon.cola.platform.tianquan.jianshen.admin.audit.repository.jdbc.PostgresqlAuditRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PostgresqlAuditStoreTest {

    @BeforeAll
    static void bindTheProcessWideEngine() {
        SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
    }

    @Test
    void returnsTheSameStableIdThatWasPersisted() {
        EntityManager entityManager = mock(EntityManager.class);
        PostgresqlAuditRepository store = new PostgresqlAuditRepository(
                entityManager, mock(AuditCursorCodec.class));
        AuditVO candidate = new AuditVO(
                null, "17", "ROLE_CHANGED", "SUCCESS", "INFO", "USER",
                "31", "ROLE", "51", null, "ALLOW", "request-1", "trace-1",
                Map.of(), Map.of("status", "ACTIVE"), "sha256:evidence",
                Instant.parse("2026-07-30T12:00:00Z"));

        AuditVO persisted = store.append(candidate);

        ArgumentCaptor<AuditLogPO> entity = ArgumentCaptor.forClass(
                AuditLogPO.class);
        verify(entityManager).persist(entity.capture());
        long generatedId = entity.getValue().getId();
        assertThat(generatedId).isPositive();
        assertThat(persisted.id()).isEqualTo(String.valueOf(generatedId));
    }
}
