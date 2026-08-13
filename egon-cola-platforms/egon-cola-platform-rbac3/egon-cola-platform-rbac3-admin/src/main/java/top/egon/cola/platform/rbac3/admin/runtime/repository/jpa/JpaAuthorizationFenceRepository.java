package top.egon.cola.platform.rbac3.admin.runtime.repository.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.runtime.service.AuthorizationFenceService;
import top.egon.cola.platform.rbac3.admin.runtime.repository.redis.RedisAuthorizationRuntimeRepository;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.session.domain.enums.SessionStatusEnum;
import top.egon.cola.platform.rbac3.admin.runtime.repository.AuthorizationFenceRepository;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.AuthorizationFenceVO;

/**
 * 类型 `JpaAuthorizationFenceRepository` 位于当前包内，是类型，用于承载 `Rbac3 Authorization Fence Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `JpaAuthorizationFenceRepository` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Authorization Fence Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Expands coarse authorization mutation fences to the affected runtime sessions.
 */
@Repository
public class JpaAuthorizationFenceRepository implements AuthorizationFenceRepository {

    /**
     * 字段 `FENCE_TTL` 表示 `JpaAuthorizationFenceRepository` 中与 `FENCE TTL` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `FENCE_TTL` stores the `FENCE TTL`-related state, dependency, configuration, or result of `JpaAuthorizationFenceRepository` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `FENCE_TTL` 时应保持 `JpaAuthorizationFenceRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `FENCE_TTL`, preserve `JpaAuthorizationFenceRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Duration FENCE_TTL = Duration.ofMinutes(15);

    /**
     * 字段 `entityManager` 表示 `JpaAuthorizationFenceRepository` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `JpaAuthorizationFenceRepository` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `JpaAuthorizationFenceRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `JpaAuthorizationFenceRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;
    /**
     * 字段 `runtimeStore` 表示 `JpaAuthorizationFenceRepository` 中与 `runtime Store` 相关的状态、依赖、配置或结果（声明类型 `RedisAuthorizationRuntimeRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `runtimeStore` stores the `runtime Store`-related state, dependency, configuration, or result of `JpaAuthorizationFenceRepository` (declared type `RedisAuthorizationRuntimeRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `runtimeStore` 时应保持 `JpaAuthorizationFenceRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `runtimeStore`, preserve `JpaAuthorizationFenceRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RedisAuthorizationRuntimeRepository runtimeStore;

    /**
     * 构造器 `JpaAuthorizationFenceRepository` 用于创建并初始化 `JpaAuthorizationFenceRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `JpaAuthorizationFenceRepository` creates and initializes `JpaAuthorizationFenceRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `JpaAuthorizationFenceRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `JpaAuthorizationFenceRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param runtimeStore 输入参数 `runtimeStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public JpaAuthorizationFenceRepository(
            EntityManager entityManager,
            RedisAuthorizationRuntimeRepository runtimeStore) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.runtimeStore = Objects.requireNonNull(runtimeStore, "runtimeStore");
    }

    /**
     * 方法 `put` 按照 `JpaAuthorizationFenceRepository` 的职责处理输入，完成 `put` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `put` processes its inputs according to `JpaAuthorizationFenceRepository`'s responsibility, performs the `put` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `put` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `put`, then continue the business flow using its result, exception, or side effect.
     *
     * @param fence 输入参数 `fence`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    @Transactional(readOnly = true)
    public void put(AuthorizationFenceVO fence) {
        sessions(fence.tenantId(), fence.scopeType(), fence.scopeId()).forEach(
                sessionId -> runtimeStore.createSessionFence(
                        fence.tenantId(), sessionId, fence.mutationId(), FENCE_TTL));
    }

    /**
     * 方法 `remove` 按照 `JpaAuthorizationFenceRepository` 的职责处理输入，完成 `remove` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `remove` processes its inputs according to `JpaAuthorizationFenceRepository`'s responsibility, performs the `remove` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `remove` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `remove`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param scopeType 输入参数 `scopeType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param scopeId 输入参数 `scopeId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    @Transactional(readOnly = true)
    public void remove(String tenantId, String scopeType, String scopeId) {
        sessions(tenantId, scopeType, scopeId).forEach(
                sessionId -> runtimeStore.removeSessionFence(tenantId, sessionId));
    }

    /**
     * 方法 `sessions` 按照 `JpaAuthorizationFenceRepository` 的职责处理输入，完成 `sessions` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `sessions` processes its inputs according to `JpaAuthorizationFenceRepository`'s responsibility, performs the `sessions` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `sessions` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `sessions`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param scopeType 输入参数 `scopeType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param scopeId 输入参数 `scopeId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private List<String> sessions(String tenantId, String scopeType, String scopeId) {
        return switch (required(scopeType).toUpperCase(Locale.ROOT)) {
            case "SESSION" -> List.of(required(scopeId));
            case "USER" -> activeSessions(tenantId, Long.valueOf(required(scopeId)));
            case "TENANT" -> {
                if (!required(tenantId).equals(required(scopeId))) {
                    throw new IllegalArgumentException("tenant fence scope must match tenantId");
                }
                yield activeSessions(tenantId, null);
            }
            default -> throw new IllegalArgumentException(
                    "unsupported authorization fence scope: " + scopeType);
        };
    }

    /**
     * 方法 `activeSessions` 按照 `JpaAuthorizationFenceRepository` 的职责处理输入，完成 `active Sessions` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `activeSessions` processes its inputs according to `JpaAuthorizationFenceRepository`'s responsibility, performs the `active Sessions` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `activeSessions` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `activeSessions`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private List<String> activeSessions(String tenantId, Long userId) {
        String userPredicate = userId == null ? "" : " and s.userId = :userId";
        var query = entityManager.createQuery("""
                select s.sessionId from SessionEntity s
                 where s.tenantId = :tenantId
                   and s.status = :status
                """ + userPredicate, Long.class)
                .setParameter("tenantId", Long.valueOf(required(tenantId)))
                .setParameter("status",
                        top.egon.cola.platform.rbac3.admin.session.domain.enums.SessionStatusEnum.ACTIVE);
        if (userId != null) {
            query.setParameter("userId", userId);
        }
        return query.getResultList().stream().map(String::valueOf).toList();
    }

    /**
     * 方法 `required` 按照 `JpaAuthorizationFenceRepository` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `JpaAuthorizationFenceRepository`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("authorization fence identifier is required");
        }
        return value.trim();
    }
}
