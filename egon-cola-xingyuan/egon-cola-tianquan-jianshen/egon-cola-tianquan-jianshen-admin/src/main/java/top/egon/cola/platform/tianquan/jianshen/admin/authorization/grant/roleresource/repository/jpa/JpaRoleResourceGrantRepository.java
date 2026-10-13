package top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.convertor.RoleFieldRuleReplacementConverter;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.convertor.RoleFieldRuleReplacementConverter.RulePersistenceContextVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.policy.domain.po.FieldRulePO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.policy.domain.enums.FieldRuleStatusEnum;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.role.domain.enums.RoleStatusEnum;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.FieldAccessLevel;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.enums.RoleResourceGrantStatusEnum;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.po.RoleResourceGrantPO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.repository.RoleResourceGrantRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission.domain.enums.PermissionStatusEnum;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission.domain.po.PermissionPO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.apibinding.domain.enums.ResourceApiBindingStatusEnum;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.enums.ResourceStatusEnum;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.enums.ResourceTypeEnum;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.po.ResourcePO;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.role.domain.po.RolePO;
import top.egon.cola.platform.tianquan.jianshen.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** JPA adapter for direct role-resource grants. */
@Repository("jpaRoleResourceGrantRepository")
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class JpaRoleResourceGrantRepository implements RoleResourceGrantRepository {

    @Qualifier("jpaSharedEM_entityManagerFactory")
    private final EntityManager entityManager;
    @Qualifier("roleFieldRuleReplacementConverterImpl")
    private final RoleFieldRuleReplacementConverter fieldConverter;
    @Qualifier("defaultValidator")
    private final Validator validator;

    /** Existing resource-grant callers keep their constructor; the new writer requires configured conversion/validation ports. */
    public JpaRoleResourceGrantRepository(EntityManager entityManager) {
        this(entityManager, null, null);
    }

    @Override
    public FieldRuleReplacementResultBO replaceFieldRules(FieldRuleReplacementBO command) {
        if (fieldConverter == null || validator == null) {
            throw new Rbac3RuleViolation("ROLE_FIELD_RULE_WRITER_UNAVAILABLE");
        }
        var validation = new ValidationUtils(validator);
        validation.validate(command);
        RolePO role = entityManager.find(RolePO.class, command.roleId(), LockModeType.PESSIMISTIC_WRITE);
        if (role == null || !command.tenantId().equals(role.getTenantId()) || !command.applicationId().equals(role.getApplicationId())
                || role.getStatus() != RoleStatusEnum.ACTIVE) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        if (role.getVersion() != command.expectedRoleVersion()) {
            throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT");
        }
        Number entitlements = (Number) entityManager.createNativeQuery("""
                select count(*) from rbac3_tenant_application entitlement
                  join rbac3_application application on application.id = entitlement.application_id and application.status = 'ACTIVE'
                 where entitlement.tenant_id = :tenantId and entitlement.application_id = :applicationId and entitlement.status = 'ACTIVE'
                   and entitlement.valid_from <= :now and (entitlement.valid_to is null or entitlement.valid_to > :now)
                """).setParameter("tenantId", command.tenantId()).setParameter("applicationId", command.applicationId())
                .setParameter("now", command.now()).getSingleResult();
        if (entitlements.longValue() < 1) { throw new Rbac3RuleViolation("ROLE_APPLICATION_MISMATCH"); }
        for (FieldRuleDesiredVO desired : command.rules()) {
            requireFieldRuleFacts(command, desired);
        }
        var existing = entityManager.createQuery("select rule from FieldRuleEntity rule where rule.tenantId = :tenantId and rule.applicationId = :applicationId and rule.roleId = :roleId",
                        FieldRulePO.class)
                .setParameter("tenantId", command.tenantId()).setParameter("applicationId", command.applicationId()).setParameter("roleId", command.roleId()).getResultList();
        var byKey = new HashMap<List<Object>, FieldRulePO>();
        for (var row : existing) {
            if (byKey.put(fieldKey(row.getPermissionId(), row.getFieldDefinitionId(), row.getValidFrom()), row) != null) {
                throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT");
            }
        }
        Set<List<Object>> requested = new HashSet<>();
        int added = 0, changed = 0, disabled = 0;
        for (FieldRuleDesiredVO desired : command.rules()) {
            List<Object> key = fieldKey(desired.permissionId(), desired.fieldDefinitionId(), desired.validFrom());
            requested.add(key);
            var row = byKey.get(key);
            var access = fieldConverter.toAccessLevel(desired.accessLevel());
            if (row == null) {
                var source = fieldConverter.toRequest(desired);
                var context = new RulePersistenceContextVO(
                        SnowflakeIdGenerator.nextLongId(), command.tenantId(), command.applicationId(), command.roleId(), command.actorId(), command.now());
                var created = validation.validate(fieldConverter.toTarget(source, context));
                entityManager.persist(created);
                added++;
            } else if (row.getAccessLevel() != access || !Objects.equals(row.getValidTo(), desired.validTo()) || row.getStatus() != desired.status()) {
                row.update(access, desired.validFrom(), desired.validTo(), command.actorId(), command.now());
                row.changeStatus(desired.status(), command.actorId(), command.now());
                validation.validate(row);
                changed++;
            }
        }
        for (var row : existing) {
            if (row.getStatus() == FieldRuleStatusEnum.ACTIVE
                    && !requested.contains(fieldKey(row.getPermissionId(), row.getFieldDefinitionId(), row.getValidFrom()))) {
                row.changeStatus(FieldRuleStatusEnum.DISABLED, command.actorId(), command.now());
                validation.validate(row);
                disabled++;
            }
        }
        if (added + changed + disabled > 0) {
            role.touch(command.actorId(), command.now());
        }
        entityManager.flush();
        return new FieldRuleReplacementResultBO(role.getVersion(), command.rules().size(), added, changed, disabled);
    }

    private void requireFieldRuleFacts(FieldRuleReplacementBO command, FieldRuleDesiredVO desired) {
        PermissionPO permission = entityManager.find(PermissionPO.class, desired.permissionId(), LockModeType.PESSIMISTIC_READ);
        if (permission == null || !command.applicationId().equals(permission.getApplicationId()) || permission.getStatus() != PermissionStatusEnum.ACTIVE) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        var rows = entityManager.createNativeQuery("""
                select definition.resource_id, definition.writable, definition.default_access
                  from rbac3_field_definition definition join rbac3_resource resource
                    on resource.id = definition.resource_id and resource.application_id = definition.application_id
                 where definition.id = :fieldId and definition.application_id = :applicationId
                   and definition.status = 'ACTIVE' and resource.status = 'ACTIVE'
                   and resource.required_permission_id = :permissionId
                """).setParameter("fieldId", desired.fieldDefinitionId()).setParameter("applicationId", command.applicationId())
                .setParameter("permissionId", desired.permissionId()).getResultList();
        if (rows.size() != 1) { throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND"); }
        Object[] definition = (Object[]) rows.getFirst();
        var cap = Boolean.TRUE.equals(definition[1]) ? FieldAccessLevel.WRITE
                : FieldAccessLevel.valueOf(String.valueOf(definition[2]));
        if (accessRank(desired.accessLevel()) > accessRank(cap)) { throw new Rbac3RuleViolation("FIELD_ACCESS_DENIED"); }
        Number grants = (Number) entityManager.createNativeQuery("""
                select count(*) from rbac3_role_resource_grant grant_row
                  join rbac3_resource source on source.id = grant_row.resource_id
                    and source.application_id = grant_row.application_id and source.status = 'ACTIVE'
                 where grant_row.tenant_id = :tenantId and grant_row.application_id = :applicationId and grant_row.role_id = :roleId
                   and grant_row.status = 'ACTIVE' and grant_row.valid_from <= :now and (grant_row.valid_to is null or grant_row.valid_to > :now)
                   and (grant_row.resource_id = :resourceId or exists (
                       select 1 from rbac3_resource_api_binding binding where binding.application_id = grant_row.application_id
                         and binding.source_resource_id = grant_row.resource_id and binding.api_resource_id = :resourceId
                         and binding.status = 'ACTIVE'))
                """).setParameter("tenantId", command.tenantId()).setParameter("applicationId", command.applicationId()).setParameter("roleId", command.roleId())
                .setParameter("resourceId", definition[0]).setParameter("now", command.now()).getSingleResult();
        if (grants.longValue() < 1) { throw new Rbac3RuleViolation("FIELD_ACCESS_DENIED"); }
    }

    private static List<Object> fieldKey(Long permissionId, Long fieldId, Instant from) {
        return List.of(permissionId, fieldId, from);
    }

    private static int accessRank(FieldAccessLevel level) {
        return switch (level) {
            case NONE -> 0;
            case MASKED_READ -> 1;
            case READ -> 2;
            case WRITE -> 3;
        };
    }

    @Override
    public GrantTreeFacts loadTree(Long tenantId, Long roleId, Instant now) {
        RolePO role = entityManager.find(RolePO.class, roleId, LockModeType.PESSIMISTIC_READ);
        if (role == null || !tenantId.equals(role.getTenantId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        List<RoleResourceGrantPO> direct = entityManager.createQuery(
                        "select grant from RoleResourceGrantEntity grant "
                                + "where grant.tenantId = :tenantId and grant.roleId = :roleId "
                                + "and grant.applicationId = :applicationId "
                                + "and grant.status = :status and grant.validFrom <= :now "
                                + "and (grant.validTo is null or grant.validTo > :now)",
                        RoleResourceGrantPO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("roleId", roleId)
                .setParameter("applicationId", role.getApplicationId())
                .setParameter("status", RoleResourceGrantStatusEnum.ACTIVE)
                .setParameter("now", now)
                .getResultList();
        List<ResourcePO> catalog = entityManager.createQuery(
                        "select resource from ResourceEntity resource "
                                + "where resource.applicationId = :applicationId "
                                + "order by resource.resourceCode", ResourcePO.class)
                .setParameter("applicationId", role.getApplicationId())
                .getResultList();
        Map<Long, String> codes = new HashMap<>();
        catalog.forEach(resource -> codes.put(resource.getId(), resource.getResourceCode()));
        Map<Long, List<LinkedApi>> linkedApis = new HashMap<>();
        for (Object[] binding : entityManager.createQuery(
                        "select binding.sourceResourceId, api.id, api.resourceCode, api.resourceName "
                                + "from ResourceApiBindingEntity binding "
                                + "join ResourceEntity api on api.id = binding.apiResourceId "
                                + "and api.applicationId = binding.applicationId "
                                + "where binding.applicationId = :applicationId "
                                + "and binding.status = :status "
                                + "and api.status = :apiStatus", Object[].class)
                .setParameter("applicationId", role.getApplicationId())
                .setParameter("status", ResourceApiBindingStatusEnum.ACTIVE)
                .setParameter("apiStatus", ResourceStatusEnum.ACTIVE)
                .getResultList()) {
            linkedApis.computeIfAbsent(((Number) binding[0]).longValue(), ignored -> new ArrayList<>())
                    .add(new LinkedApi(
                            ((Number) binding[1]).longValue(), String.valueOf(binding[2]),
                            String.valueOf(binding[3]), null, null));
        }
        List<ResourceFact> facts = catalog.stream().map(resource -> {
            PermissionPO permission = resource.getRequiredPermissionId() == null
                    ? null : entityManager.find(PermissionPO.class, resource.getRequiredPermissionId());
            String mappingStatus = permission != null
                    && permission.getStatus() == PermissionStatusEnum.ACTIVE
                    ? "CONFIGURED" : "UNCONFIGURED";
            String parentCode = resource.getParentResourceId() == null ? null
                    : codes.get(resource.getParentResourceId());
            return new ResourceFact(
                    resource.getId(), resource.getResourceCode(), resource.getResourceName(),
                    category(resource.getResourceType()), resource.getResourceType().name(),
                    parentCode, resource.getStatus().name(), mappingStatus,
                    linkedApis.getOrDefault(resource.getId(), List.of()));
        }).toList();
        return new GrantTreeFacts(role.getApplicationId(), role.getVersion(), direct, facts);
    }

    @Override
    public ReplaceResult replace(ReplaceCommand command) {
        RolePO role = entityManager.find(RolePO.class, command.roleId(), LockModeType.PESSIMISTIC_WRITE);
        if (role == null
                || !command.tenantId().equals(role.getTenantId())
                || !command.applicationId().equals(role.getApplicationId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        if (role.getVersion() != command.expectedRoleVersion()) {
            throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT");
        }
        List<ResourcePO> resources = entityManager.createQuery(
                        "select resource from ResourceEntity resource "
                                + "where resource.applicationId = :applicationId "
                                + "and resource.id in :resourceIds", ResourcePO.class)
                .setParameter("applicationId", command.applicationId())
                .setParameter("resourceIds", command.resourceIds())
                .getResultList();
        if (resources.size() != command.resourceIds().size()) {
            throw new Rbac3RuleViolation("ROLE_APPLICATION_MISMATCH");
        }
        for (ResourcePO resource : resources) {
            if ((resource.getResourceType() != ResourceTypeEnum.ROUTE
                    && resource.getResourceType() != ResourceTypeEnum.ACTION
                    && resource.getResourceType() != ResourceTypeEnum.API)
                    || resource.getStatus() != ResourceStatusEnum.ACTIVE
                    || resource.getRequiredPermissionId() == null) {
                throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
            }
            PermissionPO permission = entityManager.find(
                    PermissionPO.class, resource.getRequiredPermissionId());
            if (permission == null || permission.getStatus() != PermissionStatusEnum.ACTIVE) {
                throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
            }
        }
        List<RoleResourceGrantPO> existing = entityManager.createQuery(
                        "select grant from RoleResourceGrantEntity grant "
                                + "where grant.tenantId = :tenantId and grant.roleId = :roleId "
                                + "and grant.applicationId = :applicationId "
                                + "and grant.status = :status "
                                + "and grant.validFrom <= :now "
                                + "and (grant.validTo is null or grant.validTo > :now)",
                        RoleResourceGrantPO.class)
                .setParameter("tenantId", command.tenantId())
                .setParameter("roleId", command.roleId())
                .setParameter("applicationId", command.applicationId())
                .setParameter("status", RoleResourceGrantStatusEnum.ACTIVE)
                .setParameter("now", command.validFrom())
                .getResultList();
        Set<Long> requested = new LinkedHashSet<>(command.resourceIds());
        Set<Long> existingIds = existing.stream()
                .map(RoleResourceGrantPO::getResourceId)
                .collect(java.util.stream.Collectors.toSet());
        long removed = 0L;
        for (RoleResourceGrantPO grant : existing) {
            if (!requested.contains(grant.getResourceId())) {
                grant.disable(command.actorId(), command.validFrom().plusNanos(1));
                removed++;
            }
        }
        long added = 0L;
        for (Long resourceId : requested) {
            if (!existingIds.contains(resourceId)) {
                entityManager.persist(new RoleResourceGrantPO(
                        SnowflakeIdGenerator.nextLongId(), command.tenantId(), command.applicationId(),
                        command.roleId(), resourceId, command.validFrom(), command.validTo(),
                        command.actorId(), command.validFrom()));
                added++;
            }
        }
        role.touch(command.actorId(), command.validFrom());
        entityManager.flush();
        return new ReplaceResult(requested, role.getVersion(), added, removed);
    }

    @Override
    public Set<Long> activeDirectResourceIds(Set<Long> roleIds, Instant now) {
        return new LinkedHashSet<>(entityManager.createQuery(
                        "select grant.resourceId from RoleResourceGrantEntity grant "
                                + "where grant.roleId in :roleIds and grant.status = :status "
                                + "and grant.validFrom <= :now "
                                + "and (grant.validTo is null or grant.validTo > :now)",
                        Long.class)
                .setParameter("roleIds", roleIds)
                .setParameter("status", RoleResourceGrantStatusEnum.ACTIVE)
                .setParameter("now", now)
                .getResultList());
    }

    private static String category(ResourceTypeEnum type) {
        return switch (type) {
            case APP -> "APP";
            case MENU -> "MENU";
            case ROUTE -> "PAGE";
            case ACTION -> "ACTION";
            case API -> "API";
        };
    }
}
