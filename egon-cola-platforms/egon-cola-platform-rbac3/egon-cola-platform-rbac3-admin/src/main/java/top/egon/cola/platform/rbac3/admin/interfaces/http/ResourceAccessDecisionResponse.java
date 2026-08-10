package top.egon.cola.platform.rbac3.admin.interfaces.http;

import top.egon.cola.platform.rbac3.admin.authorization.application.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.contract.authorization.Decision;

import java.time.Instant;
import java.util.Objects;

/**
 * 最小化的用户 Resource Server 入口授权响应，不暴露角色或权限集合。
 * Minimal user Resource Server entry decision that exposes no roles or permission set.
 *
 * @param decision ALLOW 或 DENY 判定 / ALLOW or DENY decision
 * @param reasonCode 稳定原因码 / stable reason code
 * @param authVersion 用户授权版本；快照不可用时为空 /
 *                    user authorization version, or {@code null} when no snapshot is available
 * @param sessionVersion 会话授权版本；快照不可用时为空 /
 *                       session authorization version, or {@code null} when no snapshot is available
 * @param policyVersion 租户策略版本；快照不可用时为空 /
 *                      tenant policy version, or {@code null} when no snapshot is available
 * @param decidedAt 判定时间 / decision time
 */
public record ResourceAccessDecisionResponse(
        Decision decision,
        String reasonCode,
        Long authVersion,
        Long sessionVersion,
        Long policyVersion,
        Instant decidedAt) {

    /**
     * 校验最小响应及其版本完整性。
     * Validates the minimal response and the completeness of its versions.
     */
    public ResourceAccessDecisionResponse {
        decision = Objects.requireNonNull(decision, "decision");
        if (reasonCode == null || reasonCode.isBlank()) {
            throw new IllegalArgumentException("reasonCode is required");
        }
        reasonCode = reasonCode.trim();
        decidedAt = Objects.requireNonNull(decidedAt, "decidedAt");
        int versionCount = (authVersion == null ? 0 : 1)
                + (sessionVersion == null ? 0 : 1)
                + (policyVersion == null ? 0 : 1);
        if (versionCount != 0 && versionCount != 3) {
            throw new IllegalArgumentException(
                    "authorization versions must be all present or all absent");
        }
        if ((authVersion != null && authVersion < 0)
                || (sessionVersion != null && sessionVersion < 0)
                || (policyVersion != null && policyVersion < 0)) {
            throw new IllegalArgumentException(
                    "authorization versions must not be negative");
        }
    }

    /**
     * 从应用服务结果创建传输响应。
     * Creates a transport response from the application service result.
     *
     * @param result 应用服务结果 / application service result
     * @return 最小化传输响应 / minimal transport response
     */
    public static ResourceAccessDecisionResponse from(
            AuthorizationDecisionService.ResourceAccessDecision result) {
        Objects.requireNonNull(result, "result");
        return new ResourceAccessDecisionResponse(
                result.decision(), result.reasonCode(), result.authVersion(),
                result.sessionVersion(), result.policyVersion(), result.decidedAt());
    }
}
