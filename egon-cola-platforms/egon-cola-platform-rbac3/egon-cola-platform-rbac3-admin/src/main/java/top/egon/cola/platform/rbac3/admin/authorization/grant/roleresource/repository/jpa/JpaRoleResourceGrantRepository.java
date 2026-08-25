package top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.repository.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.enums.RoleResourceGrantStatusEnum;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.po.RoleResourceGrantPO;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.repository.RoleResourceGrantRepository;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** JPA adapter for direct role-resource grant rows. */
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
        List<RoleResourceGrantPO> rows = entityManager.createQuery(
                        "select grant from RoleResourceGrantEntity grant "
                                + "where grant.tenantId = :tenantId and grant.roleId = :roleId "
                                + "and grant.status = :status and grant.validFrom <= :now "
                                + "and (grant.validTo is null or grant.validTo > :now)",
                        RoleResourceGrantPO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("roleId", roleId)
                .setParameter("status", RoleResourceGrantStatusEnum.ACTIVE)
                .setParameter("now", now)
                .getResultList();
        Long applicationId = rows.isEmpty() ? 1L : rows.getFirst().getApplicationId();
        return new GrantTreeFacts(applicationId, 0L, rows);
    }

    @Override
    public ReplaceResult replace(ReplaceCommand command) {
        Set<Long> requested = new LinkedHashSet<>(command.resourceIds());
        for (Long resourceId : requested) {
            entityManager.persist(new RoleResourceGrantPO(
                    idGenerator.nextLongId(), command.tenantId(), command.applicationId(),
                    command.roleId(), resourceId, command.validFrom(), command.validTo(),
                    command.actorId(), command.validFrom()));
        }
        return new ReplaceResult(requested, command.expectedRoleVersion() + 1L);
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
}
