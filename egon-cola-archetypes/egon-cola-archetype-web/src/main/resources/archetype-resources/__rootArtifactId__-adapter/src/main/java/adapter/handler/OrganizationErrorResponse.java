package ${package}.adapter.handler;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record OrganizationErrorResponse(
        String code,
        String message,
        String traceId,
        Instant timestamp,
        Map<String, List<String>> fieldErrors) {
    public OrganizationErrorResponse { fieldErrors = Map.copyOf(fieldErrors); }
}
