package top.egon.cola.component.gateway.admin.application.credential;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GatewayCredentialStore {

    void insert(CredentialRecord credential);

    Optional<CredentialRecord> find(String applicationId, String keyId);

    Optional<CredentialRecord> findByAccessKey(String accessKey);

    List<CredentialRecord> list(String applicationId);

    void overlap(String id, Instant validUntil, Instant now);

    void revoke(String id, Instant now);

    record CredentialRecord(
            String id,
            String applicationId,
            String accessKey,
            String secretCiphertext,
            String keyVersion,
            String status,
            Instant validFrom,
            Instant validUntil,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
