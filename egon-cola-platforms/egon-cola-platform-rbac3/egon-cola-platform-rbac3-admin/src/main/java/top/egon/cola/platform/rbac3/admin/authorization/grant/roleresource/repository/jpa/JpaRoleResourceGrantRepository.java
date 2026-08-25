package top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.enums.RoleResourceGrantStatusEnum;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.po.RoleResourceGrantPO;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.repository.RoleResourceGrantRepository;
import top.egon.cola.platform.rbac3.admin.authorization.permission.domain.enums.PermissionStatusEnum;
import top.egon.cola.platform.rbac3.admin.authorization.permission.domain.po.PermissionPO;
import top.egon.cola.platform.rbac3.admin.authorization.resource.apibinding.domain.enums.ResourceApiBindingStatusEnum;
import top.egon.cola.platform.rbac3.admin.authorization.resource.domain.enums.ResourceStatusEnum;
import top.egon.cola.platform.rbac3.admin.authorization.resource.domain.enums.ResourceTypeEnum;
import top.egon.cola.platform.rbac3.admin.authorization.resource.domain.po.ResourcePO;
import top.egon.cola.platform.rbac3.admin.iam.role.domain.po.RolePO;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

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
@Repository
public class JpaRoleResourceGrantRepository implements RoleResourceGrantRepository {

    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;

    public JpaRoleResourceGrantRepository(
            EntityManager entityManager,
            LongIdGenerator idGenerator) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
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
                        idGenerator.nextLongId(), command.tenantId(), command.applicationId(),
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
