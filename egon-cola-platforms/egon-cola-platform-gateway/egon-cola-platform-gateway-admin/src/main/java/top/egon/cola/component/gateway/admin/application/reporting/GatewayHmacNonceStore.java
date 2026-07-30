package top.egon.cola.component.gateway.admin.application.reporting;

import java.time.Instant;

public interface GatewayHmacNonceStore {

    boolean claim(
            String accessKey,
            String nonce,
            Instant expiresAt,
            Instant now);

    int deleteExpired(Instant now);
}
