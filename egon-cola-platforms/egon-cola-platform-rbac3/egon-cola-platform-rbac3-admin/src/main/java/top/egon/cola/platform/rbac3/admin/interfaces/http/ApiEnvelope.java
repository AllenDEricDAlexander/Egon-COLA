package top.egon.cola.platform.rbac3.admin.interfaces.http;

import java.time.Instant;

public record ApiEnvelope<T>(T data, Meta meta) {

    public record Meta(String requestId, String traceId, Instant timestamp) {
    }
}
