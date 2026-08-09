package top.egon.cola.component.gateway.mcp.transport;

import org.reactivestreams.Publisher;

import java.time.Duration;
import java.time.Instant;

/**
 * Shared MCP session state. Credentials and request bodies must never be stored.
 */
public interface McpSessionStore {

    Publisher<Void> create(Session session, Duration ttl);

    Publisher<Session> find(String sessionId);

    Publisher<Void> touch(String sessionId, Duration ttl);

    Publisher<Boolean> delete(String sessionId);

    record Session(
            String sessionId,
            String serverCode,
            String subjectId,
            String tenantId,
            String clientId,
            Instant createdAt
    ) {

        public Session {
            sessionId = required(sessionId, "sessionId");
            serverCode = required(serverCode, "serverCode");
            subjectId = required(subjectId, "subjectId");
            tenantId = required(tenantId, "tenantId");
            clientId = required(clientId, "clientId");
            createdAt = java.util.Objects.requireNonNull(
                    createdAt,
                    "createdAt"
            );
        }

        private static String required(String value, String field) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field + " is required");
            }
            return value.trim();
        }
    }
}
