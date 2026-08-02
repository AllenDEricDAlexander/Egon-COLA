package top.egon.cola.component.gateway.core.mcp.security;

import org.reactivestreams.Publisher;

import java.util.Objects;

@FunctionalInterface
public interface McpApprovalPort {

    Publisher<Result> consume(ConsumptionRequest request);

    enum Result {
        APPROVED,
        MISMATCH,
        CONSUMED,
        UNAVAILABLE
    }

    record ConsumptionRequest(
            String tokenDigest,
            String subjectId,
            String tenantId,
            String clientId,
            String serverCode,
            String toolName,
            String argumentDigest
    ) {

        public ConsumptionRequest {
            tokenDigest = digest(tokenDigest, "tokenDigest");
            subjectId = required(subjectId, "subjectId");
            tenantId = required(tenantId, "tenantId");
            clientId = required(clientId, "clientId");
            serverCode = required(serverCode, "serverCode");
            toolName = required(toolName, "toolName");
            argumentDigest = digest(argumentDigest, "argumentDigest");
        }

        private static String digest(String value, String field) {
            String digest = required(value, field);
            if (digest.length() != 64) {
                throw new IllegalArgumentException(
                        field + " must contain 64 characters"
                );
            }
            return digest;
        }

        private static String required(String value, String field) {
            Objects.requireNonNull(value, field);
            if (value.isBlank()) {
                throw new IllegalArgumentException(field + " is required");
            }
            return value.trim();
        }
    }
}
