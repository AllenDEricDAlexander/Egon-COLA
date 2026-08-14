package top.egon.cola.platform.rbac3.admin.authorization.repository;

/**
 * 用户授权传播 Fence 校验端口。
 * Port for checking a user authorization propagation fence.
     * 语义与用法：将 `FenceVerifier` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `FenceVerifier` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface FenceVerifier {

        /**
         * 判断指定用户主体是否仍处于传播 Fence 中。
         * Determines whether the specified user subject is still fenced.
         *
         * @param tenantId 租户标识 / tenant identifier
         * @param identitySub IdP 稳定主体标识 / stable IdP subject
         * @return 若存在 Fence 则为 {@code true} / {@code true} when fenced
         * 用法：调用 `isFenced` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `isFenced`, then continue the business flow using its result, exception, or side effect.
         */
        boolean isFenced(String tenantId, String identitySub);
    }
