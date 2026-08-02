package top.egon.cola.platform.idp.core.audit;

import java.time.Instant;

public record IdentitySecurityEvent(
        String eventType,
        String identitySub,
        String reason,
        String sourceBucket,
        long tokenVersion,
        Instant occurredAt
) {
}
