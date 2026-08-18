package top.egon.cola.platform.rbac3.admin.bootstrap.repository.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.bootstrap.repository.BootstrapSnapshotRepository;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.po.UserPO;
import top.egon.cola.platform.rbac3.admin.iam.role.domain.po.RolePO;
import top.egon.cola.platform.rbac3.admin.runtime.repository.redis.RedisAuthorizationRuntimeRepository;
import top.egon.cola.platform.rbac3.contract.activation.ActivationRoot;
import top.egon.cola.platform.rbac3.contract.auth.BootstrapView;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.authorization.FieldPolicyDecision;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** 启动授权快照的 JPA/运行时查询适配器。 JPA/runtime query adapter for bootstrap snapshots. */
@Repository
public class JpaBootstrapSnapshotRepository implements BootstrapSnapshotRepository {

    /** JPA 实体管理器。 JPA entity manager. */
    private final EntityManager entityManager;
    /** Redis 授权运行时存储。 Redis authorization runtime store. */
    private final RedisAuthorizationRuntimeRepository runtimeStore;

    /**
     * 创建启动快照查询适配器。 Creates the bootstrap snapshot query adapter.
     *
     * @param entityManager JPA 实体管理器；JPA entity manager
     * @param runtimeStore Redis 授权运行时存储；Redis authorization runtime store
     */
    public JpaBootstrapSnapshotRepository(
            EntityManager entityManager,
            RedisAuthorizationRuntimeRepository runtimeStore) {
        this.entityManager = entityManager;
        this.runtimeStore = runtimeStore;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Optional<BootstrapView> find(
            String tenantId,
            String userId,
            String identitySub) {
        UserPO user = requireUser(Long.valueOf(tenantId), Long.valueOf(userId));
        var record = runtimeStore.load(tenantId, identitySub);
        if (!userId.equals(record.userId())) {
            return Optional.empty();
        }
        var snapshot = record.snapshot();
        var contexts = new ArrayList<BootstrapView.ActiveRoleContext>();
        var permissions = new LinkedHashSet<String>();
        var resources = new ArrayList<String>();
        var fieldPolicies = new LinkedHashMap<String, FieldPolicyDecision>();
        for (AppAuthorizationContext context : snapshot.appContexts()) {
            permissions.addAll(context.permissions());
                resources.addAll(context.resourceCodes());
            fieldPolicies.putAll(context.fieldPolicies());
            for (String rootRoleId : context.activationRootRoleIds()) {
                RolePO role = entityManager.find(RolePO.class, Long.valueOf(rootRoleId));
                if (role == null || !role.getTenantId().equals(Long.valueOf(tenantId))) {
                    throw new Rbac3RuleViolation("AUTH_SNAPSHOT_NOT_READY");
                }
                contexts.add(new BootstrapView.ActiveRoleContext(
                        context.applicationCode(),
                        new ActivationRoot(
                                rootRoleId, role.getApplicationId().toString(),
                                role.getRoleCode()),
                        context.effectiveRoleIds(), context.eligibleAssignmentIds(),
                        context.landingRouteCode()));
            }
        }
        String defaultApplication = contexts.isEmpty()
                ? null : contexts.getFirst().applicationCode();
        String defaultRoute = contexts.stream()
                .map(BootstrapView.ActiveRoleContext::landingRoute)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        return Optional.of(new BootstrapView(
                new BootstrapView.User(
                        userId, tenantId, user.getIdentitySub(), user.getStatus().name()),
                contexts, permissions,
                resources, resources, resources, resources,
                fieldPolicies, defaultApplication, defaultRoute,
                snapshot.authVersion(),
                snapshot.policyVersion()));
    }

    /** 按租户校验并读取用户。 Loads a user after enforcing tenant ownership. */
    private UserPO requireUser(Long tenantId, Long userId) {
        UserPO user = entityManager.find(UserPO.class, userId);
        if (user == null || !tenantId.equals(user.getTenantId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return user;
    }

}
