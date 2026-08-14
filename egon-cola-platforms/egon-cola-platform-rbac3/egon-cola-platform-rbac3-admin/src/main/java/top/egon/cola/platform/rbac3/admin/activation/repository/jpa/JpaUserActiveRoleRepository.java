package top.egon.cola.platform.rbac3.admin.activation.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.activation.domain.dto.ReplaceCommandDTO;
import top.egon.cola.platform.rbac3.admin.activation.domain.po.UserActiveRolePO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.CurrentStateVO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.ResolvedActivationVO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.TransactionResultVO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.UserAuthorizationStateVO;
import top.egon.cola.platform.rbac3.admin.activation.repository.ActivationTransaction;
import top.egon.cola.platform.rbac3.admin.activation.repository.ReselectionRepository;
import top.egon.cola.platform.rbac3.admin.identity.domain.po.UserPO;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationCandidateResolver;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * JPA transaction for user-scoped active-role state.
 */
@Repository
public class JpaUserActiveRoleRepository implements ActivationTransaction, ReselectionRepository {

    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;

    public JpaUserActiveRoleRepository(EntityManager entityManager, LongIdGenerator idGenerator) {
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
    }

    @Override
    @Transactional
    public TransactionResultVO replace(
            ReplaceCommandDTO command,
            Instant now,
            Function<UserAuthorizationStateVO, ResolvedActivationVO> resolutionFactory) {
        UserPO user = lockUser(command.tenantId(), command.userId());
        if (!command.identitySub().equals(user.getIdentitySub())) {
            throw new IllegalArgumentException("identity subject does not match RBAC user");
        }
        if (user.getAuthVersion() != command.expectedAuthVersion()) {
            throw new IllegalStateException("ROLE_ACTIVATION_VERSION_CONFLICT");
        }

        List<UserActiveRolePO> existing = activeRoles(user.getTenantId(), user.getId());
        Map<String, Set<String>> current = roots(existing);
        UserAuthorizationStateVO state = new UserAuthorizationStateVO(
                command.tenantId(), command.userId(), current,
                user.getAuthVersion(), 0L, "unavailable", current.isEmpty(),
                now.plus(Duration.ofHours(12)));
        ResolvedActivationVO resolved = resolutionFactory.apply(state);
        Map<String, Set<String>> requested = resolved.resolution().activeRoleSet().rootsByApplication();
        boolean unchanged = current.equals(requested)
                && user.getAuthVersion() == resolved.facts().authVersion();
        if (unchanged) {
            return result(resolved, false, command.commandId(), current,
                    user.getAuthVersion());
        }

        existing.forEach(entityManager::remove);
        Map<String, Set<String>> assignmentIdsByRoot = new RoleActivationCandidateResolver()
                .resolve(resolved.facts().assignments(), resolved.facts().hierarchy(), now);
        for (Map.Entry<String, Set<String>> application : requested.entrySet()) {
            Long applicationId = Long.valueOf(application.getKey());
            for (String rootRoleId : application.getValue()) {
                Set<String> assignmentIds = assignmentIdsByRoot.get(rootRoleId);
                if (assignmentIds == null || assignmentIds.isEmpty()) {
                    throw new IllegalStateException(
                            "missing eligible assignment evidence for root " + rootRoleId);
                }
                entityManager.persist(new UserActiveRolePO(
                        user.getTenantId(), user.getId(), applicationId,
                        Long.valueOf(rootRoleId), new ArrayList<>(assignmentIds), now));
            }
        }
        long authVersion = user.incrementAuthVersion(command.actorId(), now);
        return result(resolved, true, command.commandId(), requested, authVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentStateVO current(
            String tenantId,
            String identitySub,
            String userId,
            Instant now) {
        UserPO user = lockUser(tenantId, userId);
        if (!identitySub.equals(user.getIdentitySub())) {
            throw new IllegalArgumentException("identity subject does not match RBAC user");
        }
        return new CurrentStateVO(
                roots(activeRoles(user.getTenantId(), user.getId())),
                user.getAuthVersion(),
                0L,
                "unavailable",
                false);
    }

    @Override
    @Transactional
    public void requireReselection(
            String tenantId,
            String userId,
            long expectedAuthVersion,
            Instant now,
            String actorId) {
        UserPO user = lockUser(tenantId, userId);
        if (user.getAuthVersion() != expectedAuthVersion) {
            throw new IllegalStateException("user authorization version conflict");
        }
        activeRoles(user.getTenantId(), user.getId()).forEach(entityManager::remove);
        user.incrementAuthVersion(actorId, now);
    }

    private TransactionResultVO result(
            ResolvedActivationVO resolved,
            boolean changed,
            String mutationId,
            Map<String, Set<String>> roots,
            long authVersion) {
        return new TransactionResultVO(
                resolved,
                changed,
                mutationId,
                roots,
                authVersion,
                resolved.facts().policyVersion(),
                resolved.resolution().snapshot().checksum(),
                Instant.now().plus(Duration.ofHours(12)));
    }

    private UserPO lockUser(String tenantId, String userId) {
        UserPO user = entityManager.find(
                UserPO.class, Long.valueOf(userId), LockModeType.PESSIMISTIC_WRITE);
        if (user == null || !Long.valueOf(tenantId).equals(user.getTenantId())) {
            throw new IllegalArgumentException("RBAC user not found");
        }
        return user;
    }

    private List<UserActiveRolePO> activeRoles(Long tenantId, Long userId) {
        return entityManager.createQuery("""
                        select active from UserActiveRoleEntity active
                        where active.tenantId = :tenantId and active.userId = :userId
                        order by active.applicationId, active.rootRoleId
                        """, UserActiveRolePO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("userId", userId)
                .getResultList();
    }

    private Map<String, Set<String>> roots(List<UserActiveRolePO> entities) {
        Map<String, Set<String>> roots = new LinkedHashMap<>();
        for (UserActiveRolePO entity : entities) {
            roots.computeIfAbsent(entity.getApplicationId().toString(), ignored -> new LinkedHashSet<>())
                    .add(entity.getRootRoleId().toString());
        }
        return roots;
    }
}
