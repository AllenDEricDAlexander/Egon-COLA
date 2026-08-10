package top.egon.cola.platform.idp.admin.audit.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import top.egon.cola.platform.idp.admin.audit.domain.dto.IdentityAuditQueryDTO;
import top.egon.cola.platform.idp.admin.audit.domain.pojo.IdentityAuditLogEntity;
import top.egon.cola.platform.idp.admin.audit.domain.vo.IdentityAuditPageVO;
import top.egon.cola.platform.idp.admin.audit.repo.IdentityAuditLogRepository;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentityAuditServiceImplTest {

    private final IdentityAuditLogRepository audits =
            mock(IdentityAuditLogRepository.class);
    private final IdentityAuditServiceImpl service =
            new IdentityAuditServiceImpl(audits);

    @Test
    void returnsSafePagedAuditFields() {
        IdentityAuditLogEntity audit = IdentityAuditLogEntity.record(
                "audit-1",
                "IDENTITY_LOGIN_SUCCEEDED",
                "admin-sub",
                "alice-sub",
                "SUCCESS",
                "AUTHENTICATED",
                "{\"internal\":\"must-not-leak\"}",
                Instant.parse("2026-08-02T00:00:00Z")
        );
        when(audits.findAll(any(Pageable.class))).thenReturn(
                new PageImpl<>(List.of(audit))
        );

        IdentityAuditPageVO result = service.list(
                new IdentityAuditQueryDTO(0, 20)
        );

        assertThat(result.content()).singleElement().satisfies(view -> {
            assertThat(view.id()).isEqualTo("audit-1");
            assertThat(view.eventType())
                    .isEqualTo("IDENTITY_LOGIN_SUCCEEDED");
            assertThat(view.reason()).isEqualTo("AUTHENTICATED");
        });
        assertThat(result.size()).isEqualTo(20);
    }

    @Test
    void rejectsInvalidPageRequest() {
        assertThatThrownBy(() -> service.list(
                new IdentityAuditQueryDTO(-1, 50)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid audit page request");
    }
}
