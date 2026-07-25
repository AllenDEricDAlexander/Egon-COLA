package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.gateway.admin.application.IdempotencyStore;

import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcIdempotencyStore implements IdempotencyStore {

    private final JdbcTemplate jdbc;

    private final ObjectMapper objectMapper;

    public JdbcIdempotencyStore(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Record> find(
            String scopeType,
            String scopeId,
            String key) {
        return jdbc.query("""
                SELECT scope_type, scope_id, idempotency_key, payload_sha256,
                       resource_id, response_content::text AS response_content,
                       created_at, expires_at
                  FROM gateway_idempotency_record
                 WHERE scope_type = ? AND scope_id = ?
                   AND idempotency_key = ?
                """, (result, row) -> new Record(
                result.getString("scope_type"),
                result.getString("scope_id"),
                result.getString("idempotency_key"),
                result.getString("payload_sha256"),
                result.getString("resource_id"),
                map(result.getString("response_content")),
                result.getTimestamp("created_at").toInstant(),
                result.getTimestamp("expires_at") == null
                        ? null
                        : result.getTimestamp("expires_at").toInstant()
        ), scopeType, scopeId, key).stream().findFirst();
    }

    @Override
    public void save(Record record) {
        jdbc.update("""
                INSERT INTO gateway_idempotency_record(
                    scope_type, scope_id, idempotency_key, payload_sha256,
                    resource_id, response_content, created_at, expires_at
                ) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?)
                """,
                record.scopeType(),
                record.scopeId(),
                record.key(),
                record.payloadSha256(),
                record.resourceId(),
                json(record.response()),
                record.createdAt(),
                record.expiresAt()
        );
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "idempotency response cannot be serialized",
                    failure
            );
        }
    }

    private Map<String, Object> map(String value) {
        try {
            return objectMapper.readValue(
                    value,
                    new TypeReference<Map<String, Object>>() {
                    }
            );
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "stored idempotency response is invalid",
                    failure
            );
        }
    }
}
