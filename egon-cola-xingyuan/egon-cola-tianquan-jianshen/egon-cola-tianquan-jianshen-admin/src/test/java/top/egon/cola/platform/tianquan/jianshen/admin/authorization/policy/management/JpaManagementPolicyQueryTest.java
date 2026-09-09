package top.egon.cola.platform.tianquan.jianshen.admin.authorization.policy.management;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleinheritance.domain.po.RoleClosurePO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.userrole.domain.po.UserRoleAssignmentPO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.policy.management.domain.po.ManagementPolicyPO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.policy.management.domain.po.ManagementSubjectPO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.policy.management.repository.jpa.JpaManagementPolicyRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.position.snapshot.domain.po.UserPositionSnapshotPO;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.user.domain.enums.UserStatusEnum;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.user.domain.po.UserPO;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaManagementPolicyQueryTest {

    @Test
    @SuppressWarnings("unchecked")
    void parsesManageableUsersAgainstTheIdentityOnlyUserEntity() {
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .applySetting("hibernate.hbm2ddl.auto", "none")
                .applySetting("jakarta.persistence.validation.mode", "none")
                .build();
        try (var factory = new MetadataSources(registry)
                .addAnnotatedClass(UserPO.class)
                .buildMetadata().buildSessionFactory();
             var session = factory.openSession()) {
            EntityManager entityManager = mock(EntityManager.class);
            TypedQuery<Object[]> query = mock(TypedQuery.class, RETURNS_SELF);
            when(query.getResultList()).thenReturn(List.of());
            when(entityManager.createQuery(anyString(), eq(Object[].class)))
                    .thenAnswer(invocation -> {
                        session.createSelectionQuery(invocation.getArgument(0), Object[].class);
                        return query;
                    });
            var repository = new JpaManagementPolicyRepository(entityManager, null, null);

            assertEquals(List.of(), repository.manageableUsers(
                    "1", "2", " Subject-3 ", Instant.parse("2026-09-06T05:00:00Z")));
            verify(query).setParameter("tenantId", 1L);
            verify(query).setParameter("status", UserStatusEnum.ACTIVE);
            verify(query).setParameter("query", "subject-3");
            verify(query).setParameter("pattern", "%subject-3%");
            verify(query).setMaxResults(200);
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void parsesCandidatePoliciesAgainstActualHibernateEntityNamesWithoutADatabase() {
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .applySetting("hibernate.hbm2ddl.auto", "none")
                .applySetting("jakarta.persistence.validation.mode", "none")
                .build();
        try (var factory = new MetadataSources(registry)
                .addAnnotatedClass(ManagementPolicyPO.class)
                .addAnnotatedClass(ManagementSubjectPO.class)
                .addAnnotatedClass(UserRoleAssignmentPO.class)
                .addAnnotatedClass(RoleClosurePO.class)
                .addAnnotatedClass(UserPositionSnapshotPO.class)
                .buildMetadata().buildSessionFactory();
             var session = factory.openSession()) {
            EntityManager entityManager = mock(EntityManager.class);
            TypedQuery<ManagementPolicyPO> query = mock(TypedQuery.class, RETURNS_SELF);
            when(query.getResultList()).thenReturn(List.of());
            when(entityManager.createQuery(anyString(), eq(ManagementPolicyPO.class)))
                    .thenAnswer(invocation -> {
                        // Use Hibernate's parser; a mocked query alone cannot detect entity-name drift.
                        session.createSelectionQuery(invocation.getArgument(0), ManagementPolicyPO.class);
                        return query;
                    });
            var repository = new JpaManagementPolicyRepository(entityManager, null, null);

            assertEquals(List.of(), repository.policies(
                    "1", "2", "3", Instant.parse("2026-09-06T05:00:00Z")));
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
