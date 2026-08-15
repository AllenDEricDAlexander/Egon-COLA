package top.egon.cola.platform.rbac3.admin.iam.role.activation.repository;

import top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.vo.CurrentActivationVO;

/**
     * 类型 `CurrentActivationRepository` 位于 `ActiveRoleSetRevalidator` 内，是接口，用于承载 `Current Activation Source` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `CurrentActivationRepository` is an interface inside `ActiveRoleSetRevalidator` and carries the responsibility, state, or contract for `Current Activation Source`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `CurrentActivationRepository` 作为 `ActiveRoleSetRevalidator` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `CurrentActivationRepository` as the responsibility boundary of `ActiveRoleSetRevalidator`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface CurrentActivationRepository {
        /**
         * 方法 `current` 按照 `CurrentActivationRepository` 的职责处理输入，完成 `current` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `current` processes its inputs according to `CurrentActivationRepository`'s responsibility, performs the `current` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `current` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `current`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        CurrentActivationVO current(String tenantId, String userId, String identitySub);
    }
