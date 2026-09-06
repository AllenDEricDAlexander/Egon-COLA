package top.egon.cola.platform.idp.admin.audit.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Predicate;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.argThat;

class IdentityAuditServiceImplTest {

    private final IdentityAuditLogRepository audits =
            mock(IdentityAuditLogRepository.class);
    private final IdentityAuditServiceImpl service =
            new IdentityAuditServiceImpl(audits);

    @Test
    @SuppressWarnings("unchecked")
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
        when(audits.findAll(any(Specification.class), any(Pageable.class))).thenReturn(
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
    @SuppressWarnings("unchecked")
    void combinesEveryNonEmptyFilterBeforeDatabasePagination() {
        Root<IdentityAuditLogEntity> root = mock(Root.class, RETURNS_DEEP_STUBS);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        CriteriaQuery<?> criteria = mock(CriteriaQuery.class);
        when(audits.findAll(any(Specification.class), any(Pageable.class))).thenAnswer(invocation -> {
            Specification<IdentityAuditLogEntity> filters = invocation.getArgument(0);
            filters.toPredicate(root, criteria, builder);
            return new PageImpl<>(List.of());
        });
        Instant from = Instant.parse("2026-09-06T07:00:00Z");
        Instant to = from.plusSeconds(3600);
        service.list(new IdentityAuditQueryDTO(0, 20, " alice-sub ", "IDENTITY_LOGIN_SUCCEEDED",
                "SUCCESS", from, to, "trace-1"));

        verify(builder).equal(root.get("actorSub"), "alice-sub");
        verify(builder).equal(root.get("eventType"), "IDENTITY_LOGIN_SUCCEEDED");
        verify(builder).equal(root.get("result"), "SUCCESS");
        verify(builder).equal(root.get("traceId"), "trace-1");
        verify(builder).greaterThanOrEqualTo(root.<Instant>get("occurredAt"), from);
        verify(builder).lessThan(root.<Instant>get("occurredAt"), to);
        var conjunction = org.mockito.ArgumentCaptor.forClass(Predicate[].class);
        verify(builder).and(conjunction.capture());
        assertThat(conjunction.getValue()).hasSize(6);
        verify(audits).findAll(any(Specification.class), argThat((Pageable page) ->
                page.getPageNumber() == 0 && page.getPageSize() == 20
                        && page.getSort().getOrderFor("occurredAt").isDescending()));
    }

    @Test
    void rejectsInvalidResultsAndTimeRanges() {
        Instant from = Instant.parse("2026-09-06T07:00:00Z");
        assertThatThrownBy(() -> new IdentityAuditQueryDTO(0, 20, null, null, "UNKNOWN", null, null, null))
                .hasMessage("invalid audit result filter");
        assertThatThrownBy(() -> new IdentityAuditQueryDTO(0, 20, null, null, null, from, from, null))
                .hasMessage("invalid audit time range");
    }

    @Test
    void rejectsInvalidPageRequest() {
        assertThatThrownBy(() -> service.list(
                new IdentityAuditQueryDTO(-1, 50)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid audit page request");
    }
}
