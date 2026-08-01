package top.egon.cola.platform.rbac3.admin.application.port;

import java.util.Map;

public interface AuthorizationEventPort {

    String enqueue(AuthorizationEvent event);

    record AuthorizationEvent(
            String tenantId,
            String aggregateType,
            String aggregateId,
            String eventType,
            Map<String, String> safePayload,
            String traceId
    ) {
        public AuthorizationEvent {
            safePayload = Map.copyOf(safePayload);
        }
    }
}
