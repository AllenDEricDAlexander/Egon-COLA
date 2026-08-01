package top.egon.cola.platform.rbac3.admin.authorization.infrastructure;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.constraint.domain.OperationSodRuleEntity;
import top.egon.cola.platform.rbac3.admin.participation.application.ParticipationFacade;

import java.time.Instant;
import java.util.List;

/**
 * Reads active typed authorization constraints without evaluating arbitrary expressions.
 */
@Repository
public class AuthorizationRuleRepository
        implements ParticipationFacade.OperationSodRuleSource {

    private final EntityManager entityManager;

    public AuthorizationRuleRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationFacade.PriorActionRule> rules(
            String tenantId,
            String applicationCode,
            String businessResource,
            String laterAction,
            Instant at) {
        return entityManager.createQuery("""
                        select r.id, r.priorActionCode, r.lookbackFrom
                          from OperationSodRuleEntity r
                         where r.tenantId = :tenantId
                           and r.applicationCode = :applicationCode
                           and r.businessResource = :businessResource
                           and r.forbiddenLaterActionCode = :laterAction
                           and r.status = :status
                           and r.validFrom <= :at
                           and (r.validTo is null or r.validTo > :at)
                         order by r.id
                        """, Object[].class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("applicationCode", applicationCode)
                .setParameter("businessResource", businessResource)
                .setParameter("laterAction", laterAction)
                .setParameter("status", OperationSodRuleEntity.Status.ACTIVE)
                .setParameter("at", at)
                .getResultList().stream()
                .map(row -> new ParticipationFacade.PriorActionRule(
                        row[0].toString(), row[1].toString(),
                        row[2] == null ? Instant.EPOCH : (Instant) row[2]))
                .toList();
    }
}
