package top.egon.cola.platform.rbac3.admin.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;

import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * 类型 `JpaDatabaseClock` 位于当前包内，是类型，用于承载 `Jpa Database Clock` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `JpaDatabaseClock` is a type in its package and carries the responsibility, state, or contract for `Jpa Database Clock`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `JpaDatabaseClock` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `JpaDatabaseClock` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Component
public final class JpaDatabaseClock implements DatabaseClock {

    /**
     * 字段 `entityManager` 表示 `JpaDatabaseClock` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `JpaDatabaseClock` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `JpaDatabaseClock` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `JpaDatabaseClock`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;

    /**
     * 构造器 `JpaDatabaseClock` 用于创建并初始化 `JpaDatabaseClock` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `JpaDatabaseClock` creates and initializes `JpaDatabaseClock`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `JpaDatabaseClock` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `JpaDatabaseClock`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public JpaDatabaseClock(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 方法 `transactionNow` 按照 `JpaDatabaseClock` 的职责处理输入，完成 `transaction Now` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `transactionNow` processes its inputs according to `JpaDatabaseClock`'s responsibility, performs the `transaction Now` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `transactionNow` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `transactionNow`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public Instant transactionNow() {
        Object value = entityManager.createNativeQuery("select transaction_timestamp()")
                .getSingleResult();
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant();
        }
        throw new IllegalStateException("unsupported PostgreSQL timestamp type");
    }
}
