package top.egon.cola.platform.rbac3.admin.directory.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.directory.domain.DirectorySnapshotEntity;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.List;
import java.util.Objects;

/**
 * 类型 `DirectorySnapshotStore` 位于当前包内，是类型，用于承载 `Directory Snapshot Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `DirectorySnapshotStore` is a type in its package and carries the responsibility, state, or contract for `Directory Snapshot Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Enforces monotonic, idempotent directory snapshot ingestion per provider.
 */
@Repository
public class DirectorySnapshotStore {

    /**
     * 字段 `entityManager` 表示 `DirectorySnapshotStore` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `DirectorySnapshotStore` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `DirectorySnapshotStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `DirectorySnapshotStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;

    /**
     * 构造器 `DirectorySnapshotStore` 用于创建并初始化 `DirectorySnapshotStore` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `DirectorySnapshotStore` creates and initializes `DirectorySnapshotStore`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `DirectorySnapshotStore` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `DirectorySnapshotStore`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public DirectorySnapshotStore(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 方法 `accept` 按照 `DirectorySnapshotStore` 的职责处理输入，完成 `accept` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `accept` processes its inputs according to `DirectorySnapshotStore`'s responsibility, performs the `accept` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `accept` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `accept`, then continue the business flow using its result, exception, or side effect.
     *
     * @param incoming 输入参数 `incoming`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Transactional
    public IngestionResult accept(DirectorySnapshotEntity incoming) {
        Objects.requireNonNull(incoming, "incoming");
        List<DirectorySnapshotEntity> existing = entityManager.createQuery("""
                        select s from DirectorySnapshotEntity s
                         where s.tenantId = :tenantId
                           and s.providerCode = :providerCode
                           and s.snapshotVersion = :snapshotVersion
                        """, DirectorySnapshotEntity.class)
                .setParameter("tenantId", incoming.getTenantId())
                .setParameter("providerCode", incoming.getProviderCode())
                .setParameter("snapshotVersion", incoming.getSnapshotVersion())
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        if (!existing.isEmpty()) {
            DirectorySnapshotEntity current = existing.getFirst();
            if (current.getChecksum().equals(incoming.getChecksum())) {
                return new IngestionResult(Outcome.IDEMPOTENT, current.getId());
            }
            throw new Rbac3RuleViolation("DIRECTORY_SNAPSHOT_CONFLICT");
        }
        Long maximum = entityManager.createQuery("""
                        select max(s.snapshotVersion) from DirectorySnapshotEntity s
                         where s.tenantId = :tenantId and s.providerCode = :providerCode
                        """, Long.class)
                .setParameter("tenantId", incoming.getTenantId())
                .setParameter("providerCode", incoming.getProviderCode())
                .getSingleResult();
        if (maximum != null && incoming.getSnapshotVersion() < maximum) {
            throw new Rbac3RuleViolation("DIRECTORY_SNAPSHOT_STALE");
        }
        entityManager.persist(incoming);
        return new IngestionResult(Outcome.ACCEPTED, incoming.getId());
    }

    /**
     * 类型 `IngestionResult` 位于 `DirectorySnapshotStore` 内，是记录类型，用于承载 `Ingestion Result` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `IngestionResult` is a record inside `DirectorySnapshotStore` and carries the responsibility, state, or contract for `Ingestion Result`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `IngestionResult` 作为 `DirectorySnapshotStore` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `IngestionResult` as the responsibility boundary of `DirectorySnapshotStore`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param outcome 记录组件 `outcome` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `outcome` carries constructor data whose meaning is defined by the record contract.
     * @param snapshotId 记录组件 `snapshotId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `snapshotId` carries constructor data whose meaning is defined by the record contract.
     */
    public record IngestionResult(/**
 * 字段 `outcome` 表示 `IngestionResult` 中与 `outcome` 相关的状态、依赖、配置或结果（声明类型 `Outcome`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `outcome` stores the `outcome`-related state, dependency, configuration, or result of `IngestionResult` (declared type `Outcome`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `outcome` 时应保持 `IngestionResult` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `outcome`, preserve `IngestionResult`'s lifecycle, immutability, and thread-safety constraints.
 */ Outcome outcome, /**
 * 字段 `snapshotId` 表示 `IngestionResult` 中与 `snapshot Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `snapshotId` stores the `snapshot Id`-related state, dependency, configuration, or result of `IngestionResult` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `snapshotId` 时应保持 `IngestionResult` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `snapshotId`, preserve `IngestionResult`'s lifecycle, immutability, and thread-safety constraints.
 */ Long snapshotId) {
    }

    /**
     * 类型 `Outcome` 位于 `DirectorySnapshotStore` 内，是枚举，用于承载 `Outcome` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Outcome` is an enum inside `DirectorySnapshotStore` and carries the responsibility, state, or contract for `Outcome`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Outcome` 作为 `DirectorySnapshotStore` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Outcome` as the responsibility boundary of `DirectorySnapshotStore`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum Outcome {
        /**
         * 字段 `ACCEPTED` 表示 `Outcome` 中与 `ACCEPTED` 相关的状态、依赖、配置或结果（声明类型 `Outcome`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ACCEPTED` stores the `ACCEPTED`-related state, dependency, configuration, or result of `Outcome` (declared type `Outcome`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ACCEPTED` 时应保持 `Outcome` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ACCEPTED`, preserve `Outcome`'s lifecycle, immutability, and thread-safety constraints.
         */
        ACCEPTED,
        /**
         * 字段 `IDEMPOTENT` 表示 `Outcome` 中与 `IDEMPOTENT` 相关的状态、依赖、配置或结果（声明类型 `Outcome`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `IDEMPOTENT` stores the `IDEMPOTENT`-related state, dependency, configuration, or result of `Outcome` (declared type `Outcome`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `IDEMPOTENT` 时应保持 `Outcome` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `IDEMPOTENT`, preserve `Outcome`'s lifecycle, immutability, and thread-safety constraints.
         */
        IDEMPOTENT
    }
}
