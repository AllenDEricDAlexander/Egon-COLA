package top.egon.cola.component.gateway.admin.application;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public interface IdempotencyStore {

    Optional<Record> find(String scopeType, String scopeId, String key);

    void save(Record record);

    record Record(
            String scopeType,
            String scopeId,
            String key,
            String payloadSha256,
            String resourceId,
            Map<String, Object> response,
            Instant createdAt,
            Instant expiresAt
    ) {
    }
}
