package top.egon.cola.platform.rbac3.admin.authorization.resource.apibinding.repository.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.enums.RoleResourceGrantStatusEnum;
import top.egon.cola.platform.rbac3.admin.authorization.resource.apibinding.domain.enums.ResourceApiBindingStatusEnum;
import top.egon.cola.platform.rbac3.admin.authorization.resource.apibinding.domain.po.ResourceApiBindingPO;
import top.egon.cola.platform.rbac3.admin.authorization.resource.apibinding.repository.ResourceApiBindingRepository;
import top.egon.cola.platform.rbac3.admin.authorization.resource.domain.enums.ResourceStatusEnum;
import top.egon.cola.platform.rbac3.admin.authorization.resource.domain.enums.ResourceTypeEnum;
import top.egon.cola.platform.rbac3.admin.authorization.resource.domain.po.ResourcePO;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** JPA adapter for the global CI-owned ROUTE/ACTION to API relation. */
@Repository
public class JpaResourceApiBindingRepository implements ResourceApiBindingRepository {

    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;

    public JpaResourceApiBindingRepository(EntityManager entityManager, LongIdGenerator idGenerator) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    @Override
    @Transactional
    public void replaceForApplication(
            Long applicationId,
            String sourceBuildId,
            String sourceChecksum,
            Set<BindingPair> pairs,
            String actorId,
            Instant now) {
        Objects.requireNonNull(pairs, "pairs");
        Map<String, ResourcePO> resources = loadResources(applicationId, pairs);
        for (BindingPair pair : pairs) {
            ResourcePO source = resources.get(key(pair.sourceResourceId()));
            ResourcePO api = resources.get(key(pair.apiResourceId()));
            if (source == null || api == null
                    || !applicationId.equals(source.getApplicationId())
                    || !applicationId.equals(api.getApplicationId())
                    || (source.getResourceType() != ResourceTypeEnum.ROUTE
                    && source.getResourceType() != ResourceTypeEnum.ACTION)
                    || api.getResourceType() != ResourceTypeEnum.API
                    || source.getStatus() != ResourceStatusEnum.ACTIVE
                    || api.getStatus() != ResourceStatusEnum.ACTIVE) {
                throw new Rbac3RuleViolation("REQUEST_INVALID");
            }
        }

        List<ResourceApiBindingPO> current = entityManager.createQuery(
                        "select binding from ResourceApiBindingEntity binding "
                                + "where binding.applicationId = :applicationId",
                        ResourceApiBindingPO.class)
                .setParameter("applicationId", applicationId)
                .getResultList();
        Set<BindingPair> requested = Set.copyOf(pairs);
        for (ResourceApiBindingPO existing : current) {
            BindingPair pair = new BindingPair(
                    existing.getSourceResourceId(), existing.getApiResourceId());
            if (!requested.contains(pair)) {
                existing.disable(actorId, now);
            } else {
                existing.refreshSource(sourceBuildId, sourceChecksum, actorId, now);
            }
        }
        Set<BindingPair> currentPairs = current.stream()
                .map(value -> new BindingPair(value.getSourceResourceId(), value.getApiResourceId()))
                .collect(java.util.stream.Collectors.toSet());
        for (BindingPair pair : requested) {
            if (!currentPairs.contains(pair)) {
                entityManager.persist(new ResourceApiBindingPO(
                        idGenerator.nextLongId(), applicationId,
                        pair.sourceResourceId(), pair.apiResourceId(),
                        sourceBuildId, sourceChecksum, actorId, now));
            }
        }
    }

    @Override
    public Set<Long> apiIdsForSources(Long applicationId, Set<Long> sourceResourceIds, Instant now) {
        if (sourceResourceIds == null || sourceResourceIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(entityManager.createQuery(
                        "select distinct binding.apiResourceId from ResourceApiBindingEntity binding "
                                + "where binding.applicationId = :applicationId "
                                + "and binding.sourceResourceId in :sourceIds "
                                + "and binding.status = :status", Long.class)
                .setParameter("applicationId", applicationId)
                .setParameter("sourceIds", sourceResourceIds)
                .setParameter("status", ResourceApiBindingStatusEnum.ACTIVE)
                .getResultList());
    }

    @Override
    public long countDistinctActiveRolesDerivingApi(
            Long applicationId, Long apiResourceId, Instant now) {
        List<Long> values = entityManager.createQuery(
                        "select distinct grant.roleId from RoleResourceGrantEntity grant, "
                                + "ResourceApiBindingEntity binding "
                                + "where grant.applicationId = :applicationId "
                                + "and binding.applicationId = :applicationId "
                                + "and binding.apiResourceId = :apiResourceId "
                                + "and binding.sourceResourceId = grant.resourceId "
                                + "and grant.status = :grantStatus "
                                + "and binding.status = :bindingStatus "
                                + "and grant.validFrom <= :now "
                                + "and (grant.validTo is null or grant.validTo > :now)", Long.class)
                .setParameter("applicationId", applicationId)
                .setParameter("apiResourceId", apiResourceId)
                .setParameter("grantStatus", RoleResourceGrantStatusEnum.ACTIVE)
                .setParameter("bindingStatus", ResourceApiBindingStatusEnum.ACTIVE)
                .setParameter("now", now)
                .getResultList();
        return values.size();
    }

    private Map<String, ResourcePO> loadResources(Long applicationId, Set<BindingPair> pairs) {
        Set<Long> ids = new HashSet<>();
        for (BindingPair pair : pairs) {
            ids.add(pair.sourceResourceId());
            ids.add(pair.apiResourceId());
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<ResourcePO> values = entityManager.createQuery(
                        "select resource from ResourceEntity resource "
                                + "where resource.applicationId = :applicationId "
                                + "and resource.id in :ids", ResourcePO.class)
                .setParameter("applicationId", applicationId)
                .setParameter("ids", ids)
                .getResultList();
        Map<String, ResourcePO> result = new HashMap<>();
        values.forEach(value -> result.put(key(value.getId()), value));
        return result;
    }

    private static String key(Long value) {
        return String.valueOf(value);
    }
}
