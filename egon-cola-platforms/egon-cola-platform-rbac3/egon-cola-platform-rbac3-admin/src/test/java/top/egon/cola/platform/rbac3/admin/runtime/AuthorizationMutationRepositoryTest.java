package top.egon.cola.platform.rbac3.admin.runtime;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.runtime.domain.AuthorizationMutationEntity;
import top.egon.cola.platform.rbac3.admin.runtime.infrastructure.AuthorizationMutationRepository;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationMutationRepositoryTest {

    @Test
    void returnsTenantScopedStableIdCursorPage() {
        EntityManager entityManager = mock(EntityManager.class);
        @SuppressWarnings("unchecked")
        TypedQuery<AuthorizationMutationEntity> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(AuthorizationMutationEntity.class)))
                .thenReturn(query);
        when(query.setParameter(anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(query);
        when(query.setMaxResults(2)).thenReturn(query);
        Instant now = Instant.parse("2026-07-30T12:00:00Z");
        when(query.getResultList()).thenReturn(List.of(
                mutation(9L, 17L, 31L, now),
                mutation(8L, 17L, 30L, now.minusSeconds(1))));

        var page = new AuthorizationMutationRepository(entityManager)
                .query("17", "PREPARING", null, 1);

        assertThat(page.items()).singleElement().satisfies(item -> {
            assertThat(item.mutationId()).isEqualTo("9");
            assertThat(item.scopeType()).isEqualTo("USER");
            assertThat(item.scopeId()).isEqualTo("31");
            assertThat(item.commandId()).isEqualTo("command-9");
        });
        assertThat(page.nextCursor()).isEqualTo("9");
    }

    private AuthorizationMutationEntity mutation(
            long mutationId,
            long tenantId,
            long userId,
            Instant now) {
        return new AuthorizationMutationEntity(
                mutationId, tenantId, userId, null,
                AuthorizationMutationEntity.ScopeType.USER,
                "command-" + mutationId,
                null, null, 1L, 2L, 3L, 4L,
                "operator", now);
    }
}
