package top.egon.cola.platform.rbac3.admin.runtime.domain.exception;

import top.egon.cola.platform.rbac3.admin.runtime.repository.Rbac3RuntimePolicy;
import top.egon.cola.platform.rbac3.admin.config.properties.Rbac3AdminProperties;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import top.egon.cola.platform.rbac3.admin.runtime.repository.ddc.AtomicRbac3RuntimePolicy;

/**
     * 类型 `PolicyApplyException` 位于 `AtomicRbac3RuntimePolicy` 内，是类型，用于承载 `Policy Apply Exception` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `PolicyApplyException` is a type inside `AtomicRbac3RuntimePolicy` and carries the responsibility, state, or contract for `Policy Apply Exception`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `PolicyApplyException` 作为 `AtomicRbac3RuntimePolicy` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `PolicyApplyException` as the responsibility boundary of `AtomicRbac3RuntimePolicy`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    final public class PolicyApplyException extends IllegalArgumentException {

        /**
         * 字段 `errorCode` 表示 `PolicyApplyException` 中与 `error Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `errorCode` stores the `error Code`-related state, dependency, configuration, or result of `PolicyApplyException` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `errorCode` 时应保持 `PolicyApplyException` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `errorCode`, preserve `PolicyApplyException`'s lifecycle, immutability, and thread-safety constraints.
         */
        private final String errorCode;

        /**
         * 构造器 `PolicyApplyException` 用于创建并初始化 `PolicyApplyException` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `PolicyApplyException` creates and initializes `PolicyApplyException`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `PolicyApplyException` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `PolicyApplyException`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param errorCode 输入参数 `errorCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public PolicyApplyException(String errorCode) {
            super(errorCode);
            this.errorCode = errorCode;
        }

        /**
         * 构造器 `PolicyApplyException` 用于创建并初始化 `PolicyApplyException` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `PolicyApplyException` creates and initializes `PolicyApplyException`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `PolicyApplyException` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `PolicyApplyException`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param errorCode 输入参数 `errorCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param cause 输入参数 `cause`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public PolicyApplyException(String errorCode, Throwable cause) {
            super(errorCode, cause);
            this.errorCode = errorCode;
        }

        public String errorCode() {
            return errorCode;
        }
    }
