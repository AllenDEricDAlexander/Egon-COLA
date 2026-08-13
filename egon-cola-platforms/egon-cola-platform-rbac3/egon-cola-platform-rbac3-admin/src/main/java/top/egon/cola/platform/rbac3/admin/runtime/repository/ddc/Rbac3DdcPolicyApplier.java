package top.egon.cola.platform.rbac3.admin.runtime.repository.ddc;

import top.egon.cola.component.ddc.api.refresh.DdcConfigApplier;

import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.runtime.repository.ApplyObserver;

/**
 * 类型 `Rbac3DdcPolicyApplier` 位于当前包内，是类型，用于承载 `Rbac3 Ddc Policy Applier` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3DdcPolicyApplier` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Ddc Policy Applier`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Adapts one exact DDC key to the atomic RBAC3 runtime policy.
 */
public final class Rbac3DdcPolicyApplier implements DdcConfigApplier {

    /**
     * 字段 `key` 表示 `Rbac3DdcPolicyApplier` 中与 `key` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `key` stores the `key`-related state, dependency, configuration, or result of `Rbac3DdcPolicyApplier` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `key` 时应保持 `Rbac3DdcPolicyApplier` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `key`, preserve `Rbac3DdcPolicyApplier`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final String key;
    /**
     * 字段 `priority` 表示 `Rbac3DdcPolicyApplier` 中与 `priority` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `priority` stores the `priority`-related state, dependency, configuration, or result of `Rbac3DdcPolicyApplier` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `priority` 时应保持 `Rbac3DdcPolicyApplier` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `priority`, preserve `Rbac3DdcPolicyApplier`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final int priority;
    /**
     * 字段 `policy` 表示 `Rbac3DdcPolicyApplier` 中与 `policy` 相关的状态、依赖、配置或结果（声明类型 `AtomicRbac3RuntimePolicy`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `policy` stores the `policy`-related state, dependency, configuration, or result of `Rbac3DdcPolicyApplier` (declared type `AtomicRbac3RuntimePolicy`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `policy` 时应保持 `Rbac3DdcPolicyApplier` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `policy`, preserve `Rbac3DdcPolicyApplier`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AtomicRbac3RuntimePolicy policy;
    /**
     * 字段 `observer` 表示 `Rbac3DdcPolicyApplier` 中与 `observer` 相关的状态、依赖、配置或结果（声明类型 `ApplyObserver`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `observer` stores the `observer`-related state, dependency, configuration, or result of `Rbac3DdcPolicyApplier` (declared type `ApplyObserver`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `observer` 时应保持 `Rbac3DdcPolicyApplier` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `observer`, preserve `Rbac3DdcPolicyApplier`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ApplyObserver observer;

    /**
     * 构造器 `Rbac3DdcPolicyApplier` 用于创建并初始化 `Rbac3DdcPolicyApplier` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3DdcPolicyApplier` creates and initializes `Rbac3DdcPolicyApplier`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3DdcPolicyApplier` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3DdcPolicyApplier`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param priority 输入参数 `priority`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policy 输入参数 `policy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3DdcPolicyApplier(
            String key,
            int priority,
            AtomicRbac3RuntimePolicy policy) {
        this(key, priority, policy, ApplyObserver.noop());
    }

    /**
     * 构造器 `Rbac3DdcPolicyApplier` 用于创建并初始化 `Rbac3DdcPolicyApplier` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3DdcPolicyApplier` creates and initializes `Rbac3DdcPolicyApplier`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3DdcPolicyApplier` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3DdcPolicyApplier`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param priority 输入参数 `priority`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policy 输入参数 `policy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param observer 输入参数 `observer`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3DdcPolicyApplier(
            String key,
            int priority,
            AtomicRbac3RuntimePolicy policy,
            ApplyObserver observer) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        this.key = key;
        this.priority = priority;
        this.policy = Objects.requireNonNull(policy, "policy");
        this.observer = Objects.requireNonNull(observer, "observer");
    }

    /**
     * 方法 `apply` 按照 `Rbac3DdcPolicyApplier` 的职责处理输入，完成 `apply` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `apply` processes its inputs according to `Rbac3DdcPolicyApplier`'s responsibility, performs the `apply` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `apply` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `apply`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actualKey 输入参数 `actualKey`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param version 输入参数 `version`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    public void apply(String actualKey, String value, long version) {
        if (!key.equals(actualKey)) {
            throw new IllegalArgumentException("unexpected RBAC3 config key: " + actualKey);
        }
        try {
            policy.apply(actualKey, value, version);
            observer.recordApply(key, "success");
        } catch (RuntimeException failure) {
            observer.recordApply(key, "failed");
            throw failure;
        }
    }

    /**
     * 方法 `priority` 按照 `Rbac3DdcPolicyApplier` 的职责处理输入，完成 `priority` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `priority` processes its inputs according to `Rbac3DdcPolicyApplier`'s responsibility, performs the `priority` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `priority` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `priority`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public int priority() {
        return priority;
    }

    }
