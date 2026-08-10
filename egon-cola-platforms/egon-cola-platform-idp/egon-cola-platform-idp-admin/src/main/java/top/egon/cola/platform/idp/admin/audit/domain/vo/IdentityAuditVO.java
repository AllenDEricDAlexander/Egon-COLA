package top.egon.cola.platform.idp.admin.audit.domain.vo;

import java.time.Instant;

/**
 * 对管理端安全展示的身份审计记录。
 *
 * <p>Identity audit record exposed safely to administration clients.</p>
 */
public record IdentityAuditVO(
        String id,
        String eventType,
        String actorSub,
        String targetSub,
        String result,
        String reason,
        Instant occurredAt
) {
}
