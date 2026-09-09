package top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.enums.RoleResourceGrantStatusEnum;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission.domain.enums.PermissionStatusEnum;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission.domain.po.PermissionPO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission.repository.ResourcePermissionMappingRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.apibinding.domain.enums.ResourceApiBindingStatusEnum;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.po.ResourcePO;
import top.egon.cola.platform.tianquan.jianshen.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** JPA adapter for resource mapping locks, permission validation and usage checks. */
@Repository
public class JpaResourcePermissionMappingRepository
        implements ResourcePermissionMappingRepository {

    private final EntityManager entityManager;

    public JpaResourcePermissionMappingRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public MappingFacts loadForUpdate(Long resourceId) {
        ResourcePO resource = entityManager.find(
                ResourcePO.class, resourceId, LockModeType.PESSIMISTIC_WRITE);
        if (resource == null) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return facts(resource);
    }

    @Override
    public PermissionFacts activePermission(Long permissionId) {
        PermissionPO permission = entityManager.find(PermissionPO.class, permissionId);
        if (permission == null || permission.getStatus() != PermissionStatusEnum.ACTIVE) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return new PermissionFacts(permission.getId(), permission.getApplicationId(),
                permission.getPermissionCode(), permission.getStatus().name());
    }

    @Override
    public long countActiveRoleUsage(MappingFacts resource, Instant now) {
        Set<Long> roleIds = new HashSet<>();
        roleIds.addAll(roleIdsForDirectGrant(resource.resourceId(), resource.applicationId(), now));
        if ("API".equals(resource.resourceType())) {
            roleIds.addAll(roleIdsForBoundSource(resource.resourceId(), resource.applicationId(), now));
        }
        return roleIds.size();
    }

    @Override
    public MappingFacts updateActualMapping(
            MappingFacts resource,
            Long permissionId,
            String actorId,
            String reason,
            Instant now) {
        ResourcePO entity = entityManager.find(
                ResourcePO.class, resource.resourceId(), LockModeType.PESSIMISTIC_WRITE);
        if (entity == null || entity.getVersion() != resource.version()) {
            throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT");
        }
        entity.confirmActualPermission(permissionId, actorId, now);
        return facts(entity);
    }

    private List<Long> roleIdsForDirectGrant(Long resourceId, Long applicationId, Instant now) {
        return numbers(entityManager.createQuery(
                        "select distinct grant.roleId from RoleResourceGrantEntity grant "
                                + "join RoleEntity role on role.id = grant.roleId "
                                + "where grant.applicationId = :applicationId "
                                + "and grant.resourceId = :resourceId "
                                + "and role.status = 'ACTIVE' "
                                + "and grant.status = :status "
                                + "and grant.validFrom <= :now "
                                + "and (grant.validTo is null or grant.validTo > :now)", Long.class)
                .setParameter("applicationId", applicationId)
                .setParameter("resourceId", resourceId)
                .setParameter("status", RoleResourceGrantStatusEnum.ACTIVE)
                .setParameter("now", now).getResultList());
    }

    private List<Long> roleIdsForBoundSource(Long apiResourceId, Long applicationId, Instant now) {
        return numbers(entityManager.createQuery(
                        "select distinct grant.roleId from RoleResourceGrantEntity grant "
                                + "join RoleEntity role on role.id = grant.roleId, "
                                + "ResourceApiBindingEntity binding "
                                + "where binding.applicationId = :applicationId "
                                + "and binding.apiResourceId = :apiResourceId "
                                + "and binding.sourceResourceId = grant.resourceId "
                                + "and role.status = 'ACTIVE' "
                                + "and grant.status = :grantStatus "
                                + "and binding.status = :bindingStatus "
                                + "and grant.validFrom <= :now "
                                + "and (grant.validTo is null or grant.validTo > :now)", Long.class)
                .setParameter("applicationId", applicationId)
                .setParameter("apiResourceId", apiResourceId)
                .setParameter("grantStatus", RoleResourceGrantStatusEnum.ACTIVE)
                .setParameter("bindingStatus", ResourceApiBindingStatusEnum.ACTIVE)
                .setParameter("now", now).getResultList());
    }

    private static MappingFacts facts(ResourcePO resource) {
        return new MappingFacts(resource.getId(), resource.getApplicationId(),
                resource.getResourceCode(), resource.getResourceType().name(),
                resource.getStatus().name(), resource.getRequiredPermissionId(),
                resource.getSuggestedPermissionCode(), resource.getVersion());
    }

    private static List<Long> numbers(List<?> values) {
        return values.stream().map(value -> ((Number) value).longValue()).toList();
    }
}
