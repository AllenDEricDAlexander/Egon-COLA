package top.egon.cola.platform.idp.core.port;

/**
 * IdP 向 RBAC3 请求 USER 应用入口权限决策的端口。
 *
 * <p>Port through which IdP requests a USER application-entry decision from RBAC3.</p>
 */
@FunctionalInterface
public interface UserResourceAccessAuthorizationPort {

    /**
     * 获取一个最小入口权限决策。
     *
     * <p>Obtains a minimal entry-permission decision.</p>
     *
     * @param request USER 入口请求；USER entry request
     * @return 仅含结论、原因和版本的决策；decision containing only outcome, reason, and versions
     */
    AccessDecision decide(AccessRequest request);

    /**
     * RBAC3 入口判定无法可靠完成时抛出的可重试异常。
     * Retryable exception raised when the RBAC3 entry decision cannot be completed reliably.
     */
    final class AccessUnavailableException extends RuntimeException {

        /** 序列化版本；serialization version. */
        private static final long serialVersionUID = 1L;

        /**
         * 创建不包含敏感传输细节的不可用异常。
         * Creates an unavailable exception without sensitive transport details.
         *
         * @param message 安全错误描述 / safe error description
         */
        public AccessUnavailableException(String message) {
            super(required(message));
        }

        /**
         * 校验异常消息。
         * Validates the exception message.
         *
         * @param value 原始消息 / raw message
         * @return 已校验消息 / validated message
         */
        private static String required(String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("message is required");
            }
            return value.trim();
        }
    }

    /**
     * USER 应用入口决策请求。
     *
     * <p>USER application-entry decision request.</p>
     *
     * @param identitySub         用户身份；user identity
     * @param tenantId           租户；tenant
     * @param sessionId          身份会话；identity session
     * @param rbacApplicationCode RBAC3 应用；RBAC3 application
     * @param entryPermissionCode 入口权限；entry permission
     */
    record AccessRequest(
            String identitySub,
            String tenantId,
            String sessionId,
            String rbacApplicationCode,
            String entryPermissionCode
    ) {
    }

    /**
     * USER 应用入口决策结果。
     *
     * <p>USER application-entry decision result.</p>
     *
     * @param decision             允许或拒绝；allow or deny
     * @param reason               稳定原因；stable reason
     * @param authorizationVersion 授权版本；authorization version
     * @param contextVersion       上下文版本；context version
     * @param policyVersion        策略版本；policy version
     */
    record AccessDecision(
            Decision decision,
            String reason,
            long authorizationVersion,
            long contextVersion,
            long policyVersion
    ) {
    }

    /**
     * 入口决策结论。
     *
     * <p>Entry-decision outcome.</p>
     */
    enum Decision {

        /**
         * 允许进入目标应用。
         *
         * <p>Allows entry to the target application.</p>
         */
        ALLOW,

        /**
         * 拒绝进入目标应用。
         *
         * <p>Denies entry to the target application.</p>
         */
        DENY
    }
}
