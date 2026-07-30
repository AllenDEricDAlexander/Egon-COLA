package top.egon.cola.platform.rbac3.admin.constraint.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.application.port.AuthorizationEventPort;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.constraint.application.ConstraintFacade;
import top.egon.cola.platform.rbac3.admin.constraint.domain.DataRuleEntity;
import top.egon.cola.platform.rbac3.admin.constraint.domain.DataRuleReferenceEntity;
import top.egon.cola.platform.rbac3.admin.constraint.domain.FieldRuleEntity;
import top.egon.cola.platform.rbac3.admin.constraint.domain.OperationSodRuleEntity;
import top.egon.cola.platform.rbac3.admin.constraint.domain.RoleCardinalityEntity;
import top.egon.cola.platform.rbac3.admin.constraint.domain.RolePrerequisiteEntity;
import top.egon.cola.platform.rbac3.admin.constraint.domain.SodMemberEntity;
import top.egon.cola.platform.rbac3.admin.constraint.domain.SodSetEntity;
import top.egon.cola.platform.rbac3.admin.identity.domain.TenantEntity;
import top.egon.cola.platform.rbac3.admin.role.domain.RoleEntity;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
public class ConstraintRepository implements
        ConstraintFacade.ConstraintStore,
        ConstraintFacade.RoleFactSource {

    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;
    private final DatabaseClock databaseClock;
    private final AuthorizationEventPort eventPort;

    public ConstraintRepository(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            DatabaseClock databaseClock,
            AuthorizationEventPort eventPort) {
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
        this.databaseClock = databaseClock;
        this.eventPort = eventPort;
    }

    @Override
    @Transactional(readOnly = true)
    public ConstraintFacade.RoleFact require(String roleId) {
        RoleEntity role = entityManager.find(RoleEntity.class, Long.valueOf(roleId));
        if (role == null) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        long parentCount = entityManager.createQuery("""
                        select count(i) from RoleInheritanceEntity i
                         where i.tenantId = :tenantId
                           and i.applicationId = :applicationId
                           and i.juniorRoleId = :roleId
                        """, Long.class)
                .setParameter("tenantId", role.getTenantId())
                .setParameter("applicationId", role.getApplicationId())
                .setParameter("roleId", role.getId())
                .getSingleResult();
        return new ConstraintFacade.RoleFact(
                roleId, role.getApplicationId().toString(), parentCount == 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConstraintFacade.SodView> sodSets(String tenantId) {
        return entityManager.createQuery("""
                        select s from SodSetEntity s
                         where s.tenantId = :tenantId order by s.setCode
                        """, SodSetEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .getResultList().stream().map(this::toSodView).toList();
    }

    @Override
    @Transactional
    public ConstraintFacade.MutationResult saveSod(
            ConstraintFacade.SaveSodCommand command) {
        Instant now = databaseClock.transactionNow();
        Long tenantId = Long.valueOf(command.tenantId());
        Long setId;
        if (command.setId() == null) {
            setId = idGenerator.nextLongId();
            entityManager.persist(new SodSetEntity(
                    setId,
                    tenantId,
                    value(command.applicationId()),
                    command.setCode(),
                    SodSetEntity.ConstraintType.valueOf(command.constraintType().name()),
                    command.maximumActiveRoles(),
                    command.validFrom(),
                    command.validTo(),
                    command.actorId(),
                    now));
        } else {
            setId = Long.valueOf(command.setId());
            SodSetEntity set = entityManager.find(
                    SodSetEntity.class, setId, LockModeType.PESSIMISTIC_WRITE);
            requireTenant(set, tenantId);
            requireVersion(set.getVersion(), command.expectedVersion());
            if (!set.getSetCode().equals(command.setCode())) {
                throw new Rbac3RuleViolation("REQUEST_INVALID");
            }
            set.update(
                    value(command.applicationId()),
                    SodSetEntity.ConstraintType.valueOf(command.constraintType().name()),
                    command.maximumActiveRoles(),
                    command.validFrom(),
                    command.validTo(),
                    command.actorId(),
                    now);
            entityManager.createQuery("""
                            delete from SodMemberEntity m
                             where m.tenantId = :tenantId and m.sodSetId = :setId
                            """)
                    .setParameter("tenantId", tenantId)
                    .setParameter("setId", setId)
                    .executeUpdate();
        }
        for (String roleId : command.roleIds()) {
            entityManager.persist(new SodMemberEntity(tenantId, setId, Long.valueOf(roleId)));
        }
        return policyMutation(command.tenantId(), "SOD_SET", setId.toString(), command.actorId(), now);
    }

    @Override
    @Transactional
    public ConstraintFacade.MutationResult savePrerequisites(
            ConstraintFacade.PrerequisiteGroupCommand command) {
        Instant now = databaseClock.transactionNow();
        Long tenantId = Long.valueOf(command.tenantId());
        RoleEntity target = entityManager.find(
                RoleEntity.class, Long.valueOf(command.targetRoleId()), LockModeType.PESSIMISTIC_WRITE);
        requireTenant(target, tenantId);
        requireVersion(target.getVersion(), command.expectedRoleVersion());
        entityManager.createQuery("""
                        delete from RolePrerequisiteEntity p
                         where p.tenantId = :tenantId
                           and p.targetRoleId = :targetRoleId
                           and p.groupCode = :groupCode
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("targetRoleId", target.getId())
                .setParameter("groupCode", command.groupCode())
                .executeUpdate();
        for (String roleId : command.prerequisiteRoleIds()) {
            entityManager.persist(new RolePrerequisiteEntity(
                    idGenerator.nextLongId(),
                    tenantId,
                    target.getId(),
                    command.groupCode(),
                    RolePrerequisiteEntity.MatchMode.valueOf(command.matchMode()),
                    Long.valueOf(roleId),
                    command.actorId(),
                    now));
        }
        target.markUpdated(command.actorId(), now);
        return policyMutation(
                command.tenantId(), "ROLE_PREREQUISITE", command.targetRoleId(),
                command.actorId(), now);
    }

    @Override
    @Transactional
    public ConstraintFacade.MutationResult saveCardinality(
            ConstraintFacade.CardinalityCommand command) {
        Instant now = databaseClock.transactionNow();
        Long tenantId = Long.valueOf(command.tenantId());
        RoleEntity role = entityManager.find(
                RoleEntity.class, Long.valueOf(command.roleId()), LockModeType.PESSIMISTIC_WRITE);
        requireTenant(role, tenantId);
        List<RoleCardinalityEntity> current = entityManager.createQuery("""
                        select c from RoleCardinalityEntity c
                         where c.tenantId = :tenantId and c.roleId = :roleId
                           and c.status = :status
                         order by c.validFrom desc
                        """, RoleCardinalityEntity.class)
                .setParameter("tenantId", tenantId)
                .setParameter("roleId", role.getId())
                .setParameter("status", RoleCardinalityEntity.Status.ACTIVE)
                .setMaxResults(1)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        String resourceId;
        if (current.isEmpty()) {
            Long id = idGenerator.nextLongId();
            entityManager.persist(new RoleCardinalityEntity(
                    id, tenantId, role.getId(),
                    RoleCardinalityEntity.ScopeType.valueOf(command.scopeType()),
                    command.maximumActive(), command.validFrom(), command.validTo(),
                    command.actorId(), now));
            resourceId = id.toString();
        } else {
            RoleCardinalityEntity cardinality = current.getFirst();
            requireVersion(cardinality.getVersion(), command.expectedVersion());
            cardinality.update(
                    RoleCardinalityEntity.ScopeType.valueOf(command.scopeType()),
                    command.maximumActive(), command.validFrom(), command.validTo(),
                    command.actorId(), now);
            resourceId = Long.toString(role.getId());
        }
        return policyMutation(
                command.tenantId(), "ROLE_CARDINALITY", resourceId, command.actorId(), now);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConstraintFacade.DataRuleView> dataRules(String tenantId) {
        return entityManager.createQuery("""
                        select r from DataRuleEntity r
                         where r.tenantId = :tenantId order by r.id
                        """, DataRuleEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .getResultList().stream().map(this::toDataRuleView).toList();
    }

    @Override
    @Transactional
    public ConstraintFacade.MutationResult saveDataRule(
            ConstraintFacade.DataRuleCommand command) {
        Instant now = databaseClock.transactionNow();
        Long tenantId = Long.valueOf(command.tenantId());
        verifySameApplicationFacts(command.tenantId(), command.applicationId(),
                command.roleId(), command.permissionId(), null);
        Long ruleId;
        if (command.ruleId() == null) {
            ruleId = idGenerator.nextLongId();
            entityManager.persist(new DataRuleEntity(
                    ruleId, tenantId, Long.valueOf(command.applicationId()),
                    Long.valueOf(command.roleId()), Long.valueOf(command.permissionId()),
                    DataRuleEntity.ScopeType.valueOf(command.scopeType()),
                    command.directorySnapshotVersion(), command.validFrom(), command.validTo(),
                    command.actorId(), now));
        } else {
            ruleId = Long.valueOf(command.ruleId());
            DataRuleEntity rule = entityManager.find(
                    DataRuleEntity.class, ruleId, LockModeType.PESSIMISTIC_WRITE);
            requireTenant(rule, tenantId);
            requireVersion(rule.getVersion(), command.expectedVersion());
            rule.update(
                    DataRuleEntity.ScopeType.valueOf(command.scopeType()),
                    command.directorySnapshotVersion(), command.validFrom(), command.validTo(),
                    command.actorId(), now);
            entityManager.createQuery("""
                            delete from DataRuleReferenceEntity r
                             where r.tenantId = :tenantId and r.dataRuleId = :ruleId
                            """)
                    .setParameter("tenantId", tenantId)
                    .setParameter("ruleId", ruleId)
                    .executeUpdate();
        }
        for (ConstraintFacade.RuleReference reference : command.references()) {
            entityManager.persist(new DataRuleReferenceEntity(
                    tenantId,
                    ruleId,
                    DataRuleReferenceEntity.ReferenceType.valueOf(reference.referenceType()),
                    Long.valueOf(reference.referenceId())));
        }
        return policyMutation(command.tenantId(), "DATA_RULE", ruleId.toString(), command.actorId(), now);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConstraintFacade.FieldRuleView> fieldRules(String tenantId) {
        return entityManager.createQuery("""
                        select r from FieldRuleEntity r
                         where r.tenantId = :tenantId order by r.id
                        """, FieldRuleEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .getResultList().stream().map(rule -> new ConstraintFacade.FieldRuleView(
                        rule.getId().toString(), rule.getApplicationId().toString(),
                        rule.getRoleId().toString(), rule.getPermissionId().toString(),
                        rule.getFieldDefinitionId().toString(), rule.getAccessLevel().name(),
                        rule.getStatus().name(), rule.getVersion())).toList();
    }

    @Override
    @Transactional
    public ConstraintFacade.MutationResult saveFieldRule(
            ConstraintFacade.FieldRuleCommand command) {
        Instant now = databaseClock.transactionNow();
        Long tenantId = Long.valueOf(command.tenantId());
        verifySameApplicationFacts(command.tenantId(), command.applicationId(),
                command.roleId(), command.permissionId(), command.fieldDefinitionId());
        Long ruleId;
        if (command.ruleId() == null) {
            ruleId = idGenerator.nextLongId();
            entityManager.persist(new FieldRuleEntity(
                    ruleId, tenantId, Long.valueOf(command.applicationId()),
                    Long.valueOf(command.roleId()), Long.valueOf(command.permissionId()),
                    Long.valueOf(command.fieldDefinitionId()),
                    FieldRuleEntity.AccessLevel.valueOf(command.accessLevel()),
                    command.validFrom(), command.validTo(), command.actorId(), now));
        } else {
            ruleId = Long.valueOf(command.ruleId());
            FieldRuleEntity rule = entityManager.find(
                    FieldRuleEntity.class, ruleId, LockModeType.PESSIMISTIC_WRITE);
            requireTenant(rule, tenantId);
            requireVersion(rule.getVersion(), command.expectedVersion());
            rule.update(
                    FieldRuleEntity.AccessLevel.valueOf(command.accessLevel()),
                    command.validFrom(), command.validTo(), command.actorId(), now);
        }
        return policyMutation(command.tenantId(), "FIELD_RULE", ruleId.toString(), command.actorId(), now);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConstraintFacade.OperationSodRuleView> operationSodRules(String tenantId) {
        return entityManager.createQuery("""
                        select r from OperationSodRuleEntity r
                         where r.tenantId = :tenantId order by r.id
                        """, OperationSodRuleEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .getResultList().stream().map(rule ->
                        new ConstraintFacade.OperationSodRuleView(
                                rule.getId().toString(), rule.getApplicationCode(),
                                rule.getBusinessResource(), rule.getPriorActionCode(),
                                rule.getForbiddenLaterActionCode(), rule.getStatus().name(),
                                rule.getVersion())).toList();
    }

    @Override
    @Transactional
    public ConstraintFacade.MutationResult saveOperationSodRule(
            ConstraintFacade.OperationSodRuleCommand command) {
        Instant now = databaseClock.transactionNow();
        Long tenantId = Long.valueOf(command.tenantId());
        Long ruleId;
        if (command.ruleId() == null) {
            ruleId = idGenerator.nextLongId();
            entityManager.persist(new OperationSodRuleEntity(
                    ruleId, tenantId, command.applicationCode(), command.businessResource(),
                    command.priorActionCode(), command.forbiddenLaterActionCode(),
                    command.lookbackFrom(), command.validFrom(), command.validTo(),
                    command.actorId(), now));
        } else {
            ruleId = Long.valueOf(command.ruleId());
            OperationSodRuleEntity rule = entityManager.find(
                    OperationSodRuleEntity.class, ruleId, LockModeType.PESSIMISTIC_WRITE);
            requireTenant(rule, tenantId);
            requireVersion(rule.getVersion(), command.expectedVersion());
            if (!rule.getApplicationCode().equals(command.applicationCode())) {
                throw new Rbac3RuleViolation("ROLE_APPLICATION_MISMATCH");
            }
            rule.update(
                    command.businessResource(), command.priorActionCode(),
                    command.forbiddenLaterActionCode(), command.lookbackFrom(),
                    command.validFrom(), command.validTo(), command.actorId(), now);
        }
        return policyMutation(
                command.tenantId(), "OPERATION_SOD_RULE", ruleId.toString(),
                command.actorId(), now);
    }

    private ConstraintFacade.SodView toSodView(SodSetEntity set) {
        List<String> roleIds = entityManager.createQuery("""
                        select m.roleId from SodMemberEntity m
                         where m.tenantId = :tenantId and m.sodSetId = :setId
                         order by m.roleId
                        """, Long.class)
                .setParameter("tenantId", set.getTenantId())
                .setParameter("setId", set.getId())
                .getResultList().stream().map(String::valueOf).toList();
        return new ConstraintFacade.SodView(
                set.getId().toString(), set.getSetCode(), set.getConstraintType().name(),
                string(set.getApplicationId()), set.getMaximumActiveRoles(), roleIds,
                set.getStatus().name(), set.getVersion());
    }

    private ConstraintFacade.DataRuleView toDataRuleView(DataRuleEntity rule) {
        List<ConstraintFacade.RuleReference> references = entityManager.createQuery("""
                        select r from DataRuleReferenceEntity r
                         where r.tenantId = :tenantId and r.dataRuleId = :ruleId
                        """, DataRuleReferenceEntity.class)
                .setParameter("tenantId", rule.getTenantId())
                .setParameter("ruleId", rule.getId())
                .getResultList().stream().map(reference -> new ConstraintFacade.RuleReference(
                        reference.getReferenceType().name(),
                        reference.getReferenceId().toString())).toList();
        return new ConstraintFacade.DataRuleView(
                rule.getId().toString(), rule.getApplicationId().toString(),
                rule.getRoleId().toString(), rule.getPermissionId().toString(),
                rule.getScopeType().name(), references, rule.getStatus().name(), rule.getVersion());
    }

    private void verifySameApplicationFacts(
            String tenantId,
            String applicationId,
            String roleId,
            String permissionId,
            String fieldDefinitionId) {
        Long count = entityManager.createQuery("""
                        select count(r) from RoleEntity r, PermissionEntity p
                         where r.id = :roleId and p.id = :permissionId
                           and r.tenantId = :tenantId and p.tenantId = :tenantId
                           and r.applicationId = :applicationId
                           and p.applicationId = :applicationId
                        """, Long.class)
                .setParameter("roleId", Long.valueOf(roleId))
                .setParameter("permissionId", Long.valueOf(permissionId))
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("applicationId", Long.valueOf(applicationId))
                .getSingleResult();
        if (count != 1L) {
            throw new Rbac3RuleViolation("ROLE_APPLICATION_MISMATCH");
        }
        if (fieldDefinitionId != null) {
            Long fieldCount = entityManager.createQuery("""
                            select count(f) from FieldDefinitionEntity f
                             where f.id = :fieldDefinitionId
                               and f.tenantId = :tenantId
                               and f.applicationId = :applicationId
                            """, Long.class)
                    .setParameter("fieldDefinitionId", Long.valueOf(fieldDefinitionId))
                    .setParameter("tenantId", Long.valueOf(tenantId))
                    .setParameter("applicationId", Long.valueOf(applicationId))
                    .getSingleResult();
            if (fieldCount != 1L) {
                throw new Rbac3RuleViolation("ROLE_APPLICATION_MISMATCH");
            }
        }
    }

    private ConstraintFacade.MutationResult policyMutation(
            String tenantId,
            String aggregateType,
            String aggregateId,
            String actorId,
            Instant now) {
        TenantEntity tenant = entityManager.find(
                TenantEntity.class, Long.valueOf(tenantId), LockModeType.PESSIMISTIC_WRITE);
        if (tenant == null) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        tenant.incrementPolicyVersion(actorId, now);
        String eventType = aggregateType + "_CHANGED";
        String propagationId = eventPort.enqueue(new AuthorizationEventPort.AuthorizationEvent(
                tenantId,
                aggregateType,
                aggregateId,
                eventType,
                Map.of("policyVersion", Long.toString(tenant.getPolicyVersion())),
                eventType.toLowerCase(java.util.Locale.ROOT) + '-' + aggregateId));
        return new ConstraintFacade.MutationResult(
                aggregateId, tenant.getPolicyVersion(), propagationId, true);
    }

    private static void requireTenant(Object entity, Long tenantId) {
        if (!(entity instanceof top.egon.cola.platform.rbac3.admin.infrastructure.persistence.TenantScopedEntity
                scoped) || !tenantId.equals(scoped.getTenantId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
    }

    private static void requireVersion(long actual, long expected) {
        if (actual != expected) {
            throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT");
        }
    }

    private static Long value(String value) {
        return value == null ? null : Long.valueOf(value);
    }

    private static String string(Long value) {
        return value == null ? null : value.toString();
    }
}
