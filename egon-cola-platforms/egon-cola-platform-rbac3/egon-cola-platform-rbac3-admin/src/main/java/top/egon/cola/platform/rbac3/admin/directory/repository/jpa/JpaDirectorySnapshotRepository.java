package top.egon.cola.platform.rbac3.admin.directory.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.directory.domain.po.DirectorySnapshotPO;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.List;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.IngestionResultVO;
import top.egon.cola.platform.rbac3.admin.directory.domain.enums.DirectorySnapshotOutcomeEnum;

/**
 * 类型 `JpaDirectorySnapshotRepository` 位于当前包内，是类型，用于承载 `Directory Snapshot Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `JpaDirectorySnapshotRepository` is a type in its package and carries the responsibility, state, or contract for `Directory Snapshot Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Enforces monotonic, idempotent directory snapshot ingestion per provider.
 */
@Repository
public class JpaDirectorySnapshotRepository {

    /**
     * 字段 `entityManager` 表示 `JpaDirectorySnapshotRepository` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `JpaDirectorySnapshotRepository` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `JpaDirectorySnapshotRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `JpaDirectorySnapshotRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;

    /**
     * 构造器 `JpaDirectorySnapshotRepository` 用于创建并初始化 `JpaDirectorySnapshotRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `JpaDirectorySnapshotRepository` creates and initializes `JpaDirectorySnapshotRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `JpaDirectorySnapshotRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `JpaDirectorySnapshotRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public JpaDirectorySnapshotRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 方法 `accept` 按照 `JpaDirectorySnapshotRepository` 的职责处理输入，完成 `accept` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `accept` processes its inputs according to `JpaDirectorySnapshotRepository`'s responsibility, performs the `accept` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `accept` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `accept`, then continue the business flow using its result, exception, or side effect.
     *
     * @param incoming 输入参数 `incoming`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Transactional
    public IngestionResultVO accept(DirectorySnapshotPO incoming) {
        Objects.requireNonNull(incoming, "incoming");
        List<DirectorySnapshotPO> existing = entityManager.createQuery("""
                        select s from DirectorySnapshotEntity s
                         where s.tenantId = :tenantId
                           and s.providerCode = :providerCode
                           and s.snapshotVersion = :snapshotVersion
                        """, DirectorySnapshotPO.class)
                .setParameter("tenantId", incoming.getTenantId())
                .setParameter("providerCode", incoming.getProviderCode())
                .setParameter("snapshotVersion", incoming.getSnapshotVersion())
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        if (!existing.isEmpty()) {
            DirectorySnapshotPO current = existing.getFirst();
            if (current.getChecksum().equals(incoming.getChecksum())) {
                return new IngestionResultVO(DirectorySnapshotOutcomeEnum.IDEMPOTENT, current.getId());
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
        return new IngestionResultVO(DirectorySnapshotOutcomeEnum.ACCEPTED, incoming.getId());
    }


    }
