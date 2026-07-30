package top.egon.cola.platform.rbac3.admin.interfaces.http;

import java.time.Instant;
import java.util.UUID;

public record ApiEnvelope<T>(T data, Meta meta) {

    public static <T> ApiEnvelope<T> success(T data) {
        String requestId = UUID.randomUUID().toString();
        return new ApiEnvelope<>(data, new Meta(requestId, requestId, Instant.now()));
    }

    public record Meta(String requestId, String traceId, Instant timestamp) {
    }
}
