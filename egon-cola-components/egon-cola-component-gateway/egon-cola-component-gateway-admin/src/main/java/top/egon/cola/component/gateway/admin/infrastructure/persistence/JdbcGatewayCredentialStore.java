package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.gateway.admin.application.credential.GatewayCredentialStore;

import java.time.Instant;
import java.util.Optional;

@Repository
public class JdbcGatewayCredentialStore implements GatewayCredentialStore {

    private final JdbcTemplate jdbc;

    public JdbcGatewayCredentialStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(CredentialRecord credential) {
        jdbc.update("""
                INSERT INTO gateway_application_credential(
                    id, application_id, access_key, secret_ciphertext,
                    secret_reference, key_version, status, valid_from,
                    valid_until, created_at, updated_at
                ) VALUES (?, ?, ?, ?, NULL, ?, ?, ?, ?, ?, ?)
                """,
                credential.id(),
                credential.applicationId(),
                credential.accessKey(),
                credential.secretCiphertext(),
                credential.keyVersion(),
                credential.status(),
                credential.validFrom(),
                credential.validUntil(),
                credential.createdAt(),
                credential.updatedAt()
        );
    }

    @Override
    public Optional<CredentialRecord> find(
            String applicationId,
            String keyId) {
        return jdbc.query("""
                SELECT id, application_id, access_key, secret_ciphertext,
                       key_version, status, valid_from, valid_until,
                       created_at, updated_at
                  FROM gateway_application_credential
                 WHERE application_id = ?
                   AND (id = ? OR access_key = ?)
                """, (result, row) -> new CredentialRecord(
                result.getString("id"),
                result.getString("application_id"),
                result.getString("access_key"),
                result.getString("secret_ciphertext"),
                result.getString("key_version"),
                result.getString("status"),
                result.getTimestamp("valid_from").toInstant(),
                result.getTimestamp("valid_until") == null
                        ? null
                        : result.getTimestamp("valid_until").toInstant(),
                result.getTimestamp("created_at").toInstant(),
                result.getTimestamp("updated_at").toInstant()
        ), applicationId, keyId, keyId).stream().findFirst();
    }

    @Override
    public void overlap(String id, Instant validUntil, Instant now) {
        jdbc.update("""
                UPDATE gateway_application_credential
                   SET status = 'ROTATING', valid_until = ?, updated_at = ?
                 WHERE id = ? AND status IN ('ACTIVE', 'ROTATING')
                """, validUntil, now, id);
    }

    @Override
    public void revoke(String id, Instant now) {
        jdbc.update("""
                UPDATE gateway_application_credential
                   SET status = 'REVOKED', valid_until = ?, updated_at = ?
                 WHERE id = ?
                """, now, now, id);
    }
}
