package top.egon.cola.platform.rbac3.admin.assignment.infrastructure;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.assignment.application.AssignmentFacade;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * 类型 `PostgresqlAssignmentLockStore` 位于当前包内，是类型，用于承载 `Postgresql Assignment Lock Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `PostgresqlAssignmentLockStore` is a type in its package and carries the responsibility, state, or contract for `Postgresql Assignment Lock Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `PostgresqlAssignmentLockStore` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `PostgresqlAssignmentLockStore` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Repository
public class PostgresqlAssignmentLockStore implements AssignmentFacade.AssignmentLock {

    /**
     * 字段 `entityManager` 表示 `PostgresqlAssignmentLockStore` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `PostgresqlAssignmentLockStore` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `PostgresqlAssignmentLockStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `PostgresqlAssignmentLockStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;

    /**
     * 构造器 `PostgresqlAssignmentLockStore` 用于创建并初始化 `PostgresqlAssignmentLockStore` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `PostgresqlAssignmentLockStore` creates and initializes `PostgresqlAssignmentLockStore`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `PostgresqlAssignmentLockStore` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `PostgresqlAssignmentLockStore`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public PostgresqlAssignmentLockStore(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    /**
     * 方法 `withLock` 按照 `PostgresqlAssignmentLockStore` 的职责处理输入，完成 `with Lock` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `withLock` processes its inputs according to `PostgresqlAssignmentLockStore`'s responsibility, performs the `with Lock` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `withLock` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `withLock`, then continue the business flow using its result, exception, or side effect.
     *
     * @param scope 输入参数 `scope`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional
    public Object withLock(AssignmentFacade.LockExecution scope) {
        String canonical = canonicalKey(
                scope.tenantId(), scope.activationRootRoleId(),
                scope.scopeType(), scope.scopeId());
        entityManager.createNativeQuery("select pg_advisory_xact_lock(:lockId)")
                .setParameter("lockId", advisoryLockId(canonical))
                .getSingleResult();
        return scope.action().get();
    }

    /**
     * 方法 `canonicalKey` 按照 `PostgresqlAssignmentLockStore` 的职责处理输入，完成 `canonical Key` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `canonicalKey` processes its inputs according to `PostgresqlAssignmentLockStore`'s responsibility, performs the `canonical Key` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `canonicalKey` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `canonicalKey`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param activationRootRoleId 输入参数 `activationRootRoleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param scopeType 输入参数 `scopeType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param scopeId 输入参数 `scopeId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public static String canonicalKey(
            String tenantId,
            String activationRootRoleId,
            String scopeType,
            String scopeId
    ) {
        return required(tenantId, "tenantId") + '|'
                + required(activationRootRoleId, "activationRootRoleId") + '|'
                + required(scopeType, "scopeType") + '|'
                + required(scopeId, "scopeId");
    }

    /**
     * 方法 `advisoryLockId` 按照 `PostgresqlAssignmentLockStore` 的职责处理输入，完成 `advisory Lock Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `advisoryLockId` processes its inputs according to `PostgresqlAssignmentLockStore`'s responsibility, performs the `advisory Lock Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `advisoryLockId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `advisoryLockId`, then continue the business flow using its result, exception, or side effect.
     *
     * @param canonicalKey 输入参数 `canonicalKey`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public static long advisoryLockId(String canonicalKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    nonBlank(canonicalKey, "canonicalKey")
                            .getBytes(StandardCharsets.UTF_8));
            long value = ByteBuffer.wrap(digest).getLong() & Long.MAX_VALUE;
            return value == 0L ? 1L : value;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    /**
     * 方法 `required` 按照 `PostgresqlAssignmentLockStore` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `PostgresqlAssignmentLockStore`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String required(String value, String name) {
        if (value == null || value.isBlank() || value.indexOf('|') >= 0) {
            throw new IllegalArgumentException(name + " is not a safe lock segment");
        }
        return value.trim();
    }

    /**
     * 方法 `nonBlank` 按照 `PostgresqlAssignmentLockStore` 的职责处理输入，完成 `non Blank` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `nonBlank` processes its inputs according to `PostgresqlAssignmentLockStore`'s responsibility, performs the `non Blank` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `nonBlank` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `nonBlank`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String nonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
