package top.egon.cola.platform.tianquan.shoubing.core.audit;

import java.time.Instant;

public record IdentitySecurityEvent(
        String eventType,
        String identitySub,
        String reason,
        String sourceBucket,
        Instant occurredAt
) {
}
