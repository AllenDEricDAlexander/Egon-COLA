package top.egon.cola.platform.rbac3.admin.interfaces.http;

import jakarta.validation.constraints.NotBlank;
import top.egon.cola.platform.rbac3.admin.authorization.application.AuthorizationDecisionService;

/**
 * 用户访问 OAuth2 Resource Server 的入口授权请求。
 * Request for deciding whether a user may enter an OAuth2 Resource Server.
 *
 * @param identitySub IdP 稳定用户主体标识 / stable IdP user subject
 * @param tid 租户标识 / tenant identifier
 * @param sid IdP 会话标识 / IdP session identifier
 * @param rbacApplicationCode 目标 Resource Server 绑定的 RBAC3 应用编码 /
 *                            RBAC3 application code bound to the target Resource Server
 * @param entryPermissionCode 进入目标应用所需的权限编码 /
 *                            permission code required to enter the target application
 */
public record ResourceAccessDecisionRequest(
        @NotBlank String identitySub,
        @NotBlank String tid,
        @NotBlank String sid,
        @NotBlank String rbacApplicationCode,
        @NotBlank String entryPermissionCode) {

    /**
     * 校验并规范化传输请求。
     * Validates and normalizes the transport request.
     */
    public ResourceAccessDecisionRequest {
        identitySub = required(identitySub, "identitySub");
        tid = required(tid, "tid");
        sid = required(sid, "sid");
        rbacApplicationCode = required(rbacApplicationCode, "rbacApplicationCode");
        entryPermissionCode = required(entryPermissionCode, "entryPermissionCode");
    }

    /**
     * 转换为授权应用服务命令。
     * Converts this transport request to an authorization application command.
     *
     * @return 资源入口授权命令 / resource-entry authorization command
     */
    public AuthorizationDecisionService.ResourceAccessRequest toCommand() {
        return new AuthorizationDecisionService.ResourceAccessRequest(
                identitySub, tid, sid, rbacApplicationCode, entryPermissionCode);
    }

    /**
     * 校验必填文本并移除首尾空白。
     * Validates required text and trims surrounding whitespace.
     *
     * @param value 待校验值 / value to validate
     * @param fieldName 字段名 / field name
     * @return 规范化文本 / normalized text
     */
    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
