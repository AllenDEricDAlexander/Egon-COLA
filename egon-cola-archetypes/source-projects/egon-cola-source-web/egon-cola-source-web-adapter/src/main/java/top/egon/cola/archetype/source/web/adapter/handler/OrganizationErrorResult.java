package top.egon.cola.archetype.source.web.adapter.handler;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record OrganizationErrorResult(
        String code,
        String message,
        String traceId,
        Instant timestamp,
        Map<String, List<String>> fieldErrors) {
    public OrganizationErrorResult { fieldErrors = Map.copyOf(fieldErrors); }
}
