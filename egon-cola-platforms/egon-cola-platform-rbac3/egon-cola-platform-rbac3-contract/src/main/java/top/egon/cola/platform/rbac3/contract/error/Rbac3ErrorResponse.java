package top.egon.cola.platform.rbac3.contract.error;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Public error envelope containing only caller-visible evidence.
 */
public record Rbac3ErrorResponse(
        Error error,
        Meta meta
) {

    public Rbac3ErrorResponse {
        error = Objects.requireNonNull(error, "error");
        meta = Objects.requireNonNull(meta, "meta");
    }

    public record Error(
            Rbac3ErrorCode code,
            String message,
            boolean retryable,
            List<Detail> details
    ) {

        public Error {
            code = Objects.requireNonNull(code, "code");
            message = required(message, "message");
            details = List.copyOf(Objects.requireNonNull(
                    details,
                    "details"
            ));
        }
    }

    public record Detail(
            String field,
            String reasonCode,
            String evidenceId
    ) {

        public Detail {
            field = required(field, "field");
            reasonCode = required(reasonCode, "reasonCode");
            evidenceId = required(evidenceId, "evidenceId");
        }
    }

    public record Meta(
            String requestId,
            String traceId,
            Instant timestamp
    ) {

        public Meta {
            requestId = required(requestId, "requestId");
            traceId = required(traceId, "traceId");
            timestamp = Objects.requireNonNull(timestamp, "timestamp");
        }
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
