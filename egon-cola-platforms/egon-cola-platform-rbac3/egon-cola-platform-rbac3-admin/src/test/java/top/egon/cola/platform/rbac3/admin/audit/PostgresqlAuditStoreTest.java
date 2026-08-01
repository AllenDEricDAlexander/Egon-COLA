package top.egon.cola.platform.rbac3.admin.audit;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.audit.application.AuditQueryService;
import top.egon.cola.platform.rbac3.admin.audit.domain.AuditLogEntity;
import top.egon.cola.platform.rbac3.admin.audit.infrastructure.AuditCursorCodec;
import top.egon.cola.platform.rbac3.admin.audit.infrastructure.PostgresqlAuditStore;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostgresqlAuditStoreTest {

    @Test
    void returnsTheSameStableIdThatWasPersisted() {
        EntityManager entityManager = mock(EntityManager.class);
        LongIdGenerator idGenerator = mock(LongIdGenerator.class);
        when(idGenerator.nextLongId()).thenReturn(42L);
        PostgresqlAuditStore store = new PostgresqlAuditStore(
                entityManager, idGenerator, mock(AuditCursorCodec.class));
        AuditQueryService.AuditView candidate = new AuditQueryService.AuditView(
                null, "17", "ROLE_CHANGED", "SUCCESS", "INFO", "USER",
                "31", "ROLE", "51", null, "ALLOW", "request-1", "trace-1",
                Map.of(), Map.of("status", "ACTIVE"), "sha256:evidence",
                Instant.parse("2026-07-30T12:00:00Z"));

        AuditQueryService.AuditView persisted = store.append(candidate);

        ArgumentCaptor<AuditLogEntity> entity = ArgumentCaptor.forClass(
                AuditLogEntity.class);
        verify(entityManager).persist(entity.capture());
        assertThat(entity.getValue().getId()).isEqualTo(42L);
        assertThat(persisted.id()).isEqualTo("42");
    }
}
