package top.egon.cola.platform.rbac3.admin.iam.role;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.state.repository.TenantAuthorizationStateRepository;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleinheritance.repository.jdbc.PostgresqlRoleClosureRepository;
import top.egon.cola.platform.rbac3.admin.iam.role.repository.jpa.JpaRoleRepository;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.repository.AuthorizationEventPublisher;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.iam.role.service.RoleFacade;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleinheritance.repository.RoleHierarchyRepository;
import top.egon.cola.platform.rbac3.admin.iam.role.repository.RoleControlRepository;
import top.egon.cola.platform.rbac3.admin.iam.role.domain.dto.CreateRoleCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.role.domain.vo.RoleMutationResultVO;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleinheritance.domain.dto.InheritanceCommandDTO;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleNode;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleControlFacadeTest {

    @Test
    void graphMutationPublishesTheVersionReturnedByAuthorizationState() {
        EntityManager entityManager = mock(EntityManager.class);
        PostgresqlRoleClosureRepository closureStore = mock(
                PostgresqlRoleClosureRepository.class);
        LongIdGenerator idGenerator = mock(LongIdGenerator.class);
        DatabaseClock databaseClock = mock(DatabaseClock.class);
        AuthorizationEventPublisher eventPort = mock(AuthorizationEventPublisher.class);
        TenantAuthorizationStateRepository stateStore = mock(
                TenantAuthorizationStateRepository.class);
        when(databaseClock.transactionNow()).thenReturn(Instant.EPOCH);
        when(stateStore.increment(10001L, "actor")).thenReturn(4L);
        when(eventPort.enqueue(any())).thenReturn("event-2");

        new JpaRoleRepository(
                entityManager,
                closureStore,
                idGenerator,
                databaseClock,
                eventPort,
                stateStore).recordGraphMutation(
                "10001", "71001", new top.egon.cola.platform.rbac3.core.hierarchy.RoleEdge(
                        "50001", "50002"), true, "actor");

        verify(stateStore).increment(10001L, "actor");
        verify(eventPort).enqueue(any());
    }

    @Test
    void roleQueriesRemainAtTheRoleControlBoundary() {
        RecordingControlStore controlStore = new RecordingControlStore();
        RoleFacade facade = new RoleFacade(new EmptyHierarchyStore(), controlStore);
        assertEquals(List.of(), facade.roles("10001", "71001"));
    }

    @Test
    void roleInheritanceRejectsRolesOutsideTheLocalApplication() {
        RoleFacade facade = new RoleFacade(new ApplicationScopedHierarchyStore());

        assertThatThrownBy(() -> facade.addInheritance(new InheritanceCommandDTO(
                "10001", "71001", "50001", "50002", -1L, "actor")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same local application");
    }

    private static final class RecordingControlStore implements RoleControlRepository {

        @Override
        public RoleMutationResultVO create(
                CreateRoleCommandDTO command,
                Instant now) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<top.egon.cola.platform.rbac3.admin.iam.role.domain.vo.RoleVO> roles(
                String tenantId,
                String applicationId) {
            return List.of();
        }
    }

    private static class EmptyHierarchyStore implements RoleHierarchyRepository {

        @Override
        public <T> T withGraphLock(
                String tenantId,
                String applicationId,
                Function<RoleHierarchy, T> action) {
            return action.apply(new RoleHierarchy(List.of(), List.of()));
        }

        @Override
        public void addEdge(String tenantId, String applicationId,
                            top.egon.cola.platform.rbac3.core.hierarchy.RoleEdge edge) {
        }

        @Override
        public void removeEdge(String tenantId, String applicationId,
                               top.egon.cola.platform.rbac3.core.hierarchy.RoleEdge edge) {
        }

        @Override
        public void rebuildClosure(String tenantId, String applicationId) {
        }
    }

    private static final class ApplicationScopedHierarchyStore extends EmptyHierarchyStore {

        @Override
        public <T> T withGraphLock(
                String tenantId,
                String applicationId,
                Function<RoleHierarchy, T> action) {
            return action.apply(new RoleHierarchy(List.of(
                    new RoleNode("50001", "71001", "SENIOR", true,
                            RoleNode.RiskLevel.LOW, false, null, 0),
                    new RoleNode("50002", "71002", "JUNIOR", true,
                            RoleNode.RiskLevel.LOW, false, null, 0)), List.of()));
        }
    }
}
