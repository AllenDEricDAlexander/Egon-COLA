package top.egon.cola.platform.rbac3.admin.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RBAC3 管理端的安全相关配置；RBAC3 不签发或签名 JWT，人员 Token 权威属于 IdP，
 * 本属性组当前只保存审计游标签名密钥。
 *
 * Security-adjacent configuration for the RBAC3 Admin. RBAC3 does not issue or
 * sign JWTs; IdP owns personnel-token authority. This group only carries the
 * audit-cursor signing secret used by the RBAC3 audit API.
 */
@ConfigurationProperties(prefix = "egon.rbac3.security")
public class Rbac3SecurityProperties {

    private String auditCursorSecretFile;

    public String getAuditCursorSecretFile() {
        return auditCursorSecretFile;
    }

    public void setAuditCursorSecretFile(String auditCursorSecretFile) {
        this.auditCursorSecretFile = auditCursorSecretFile;
    }

    public String requireAuditCursorSecretFile() {
        return required(auditCursorSecretFile, "auditCursorSecretFile");
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(fieldName + " is required");
        }
        return value.trim();
    }
}
