package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.gateway.admin.application.reporting.GatewayHmacNonceStore;

import java.time.Instant;

import static top.egon.cola.component.gateway.admin.infrastructure.persistence
        .JdbcGatewayParameters.timestamp;

@Repository
public class JdbcGatewayHmacNonceStore implements GatewayHmacNonceStore {

    private final JdbcTemplate jdbc;

    public JdbcGatewayHmacNonceStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean claim(
            String accessKey,
            String nonce,
            Instant expiresAt,
            Instant now) {
        try {
            jdbc.update("""
                    INSERT INTO gateway_hmac_nonce(
                        access_key, nonce, expires_at, created_at
                    ) VALUES (?, ?, ?, ?)
                    """, accessKey, nonce, timestamp(expiresAt),
                    timestamp(now));
            return true;
        } catch (DataIntegrityViolationException replay) {
            return false;
        }
    }

    @Override
    public int deleteExpired(Instant now) {
        return jdbc.update(
                "DELETE FROM gateway_hmac_nonce WHERE expires_at < ?",
                timestamp(now)
        );
    }
}
