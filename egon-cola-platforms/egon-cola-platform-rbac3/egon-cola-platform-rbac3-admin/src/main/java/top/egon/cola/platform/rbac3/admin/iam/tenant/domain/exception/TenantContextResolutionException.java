package top.egon.cola.platform.rbac3.admin.iam.tenant.domain.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;

/**
     * 类型 `TenantContextResolutionException` 位于 `TenantContextResolver` 内，是类型，用于承载 `Tenant Context Resolution Exception` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `TenantContextResolutionException` is a type inside `TenantContextResolver` and carries the responsibility, state, or contract for `Tenant Context Resolution Exception`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `TenantContextResolutionException` 作为 `TenantContextResolver` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `TenantContextResolutionException` as the responsibility boundary of `TenantContextResolver`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public final class TenantContextResolutionException extends RuntimeException {
        /**
         * 字段 `status` 表示 `TenantContextResolutionException` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `status` stores the `status`-related state, dependency, configuration, or result of `TenantContextResolutionException` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `status` 时应保持 `TenantContextResolutionException` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `status`, preserve `TenantContextResolutionException`'s lifecycle, immutability, and thread-safety constraints.
         */
        private final int status;
        /**
         * 字段 `reasonCode` 表示 `TenantContextResolutionException` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `TenantContextResolutionException` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `TenantContextResolutionException` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `TenantContextResolutionException`'s lifecycle, immutability, and thread-safety constraints.
         */
        private final String reasonCode;

        /**
         * 构造器 `TenantContextResolutionException` 用于创建并初始化 `TenantContextResolutionException` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `TenantContextResolutionException` creates and initializes `TenantContextResolutionException`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `TenantContextResolutionException` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `TenantContextResolutionException`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public TenantContextResolutionException(int status, String reasonCode) {
            super(reasonCode);
            this.status = status;
            this.reasonCode = reasonCode;
        }

        /**
         * 方法 `status` 按照 `TenantContextResolutionException` 的职责处理输入，完成 `status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `status` processes its inputs according to `TenantContextResolutionException`'s responsibility, performs the `status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `status` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `status`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        public int status() {
            return status;
        }

        /**
         * 方法 `reasonCode` 按照 `TenantContextResolutionException` 的职责处理输入，完成 `reason Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `reasonCode` processes its inputs according to `TenantContextResolutionException`'s responsibility, performs the `reason Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `reasonCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `reasonCode`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        public String reasonCode() {
            return reasonCode;
        }
    }
