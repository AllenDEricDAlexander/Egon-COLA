package top.egon.cola.platform.rbac3.admin.iam.role.assignment;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.iam.role.assignment.domain.po.AutoAssignmentRulePO;
import top.egon.cola.platform.rbac3.admin.iam.role.assignment.service.PositionAutoRoleRecalculator;
import top.egon.cola.platform.rbac3.admin.runtime.service.AuthorizationMutationCoordinator;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

import java.util.List;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PositionAutoRoleRecalculatorTest {

    @Test
    void doesNotOpenAuthorizationMutationWhenPositionHasNoActiveUsers() {
        EntityManager entityManager = mock(EntityManager.class);
        @SuppressWarnings("unchecked")
        TypedQuery<Long> users = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(users);
        when(users.setParameter(anyString(), any())).thenReturn(users);
        when(users.getResultList()).thenReturn(List.of());
        @SuppressWarnings("unchecked")
        TypedQuery<AutoAssignmentRulePO> rules = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(AutoAssignmentRulePO.class))).thenReturn(rules);
        when(rules.setParameter(anyString(), any())).thenReturn(rules);
        when(rules.getResultList()).thenReturn(List.of());

        AuthorizationMutationCoordinator coordinator = mock(AuthorizationMutationCoordinator.class);
        DatabaseClock clock = mock(DatabaseClock.class);
        when(clock.transactionNow()).thenReturn(Instant.parse("2026-08-15T00:00:00Z"));
        PositionAutoRoleRecalculator recalculator = new PositionAutoRoleRecalculator(
                entityManager, mock(LongIdGenerator.class), clock, coordinator);

        recalculator.recalculateForPosition(7L, 10L, "actor");

        verifyNoInteractions(coordinator);
    }
}
