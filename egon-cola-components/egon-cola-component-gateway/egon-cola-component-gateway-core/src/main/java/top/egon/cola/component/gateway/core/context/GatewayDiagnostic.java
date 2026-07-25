package top.egon.cola.component.gateway.core.context;

import java.time.Instant;
import java.util.Objects;

public record GatewayDiagnostic(
        String code,
        GatewayStage stage,
        Instant recordedAt
) {

    public GatewayDiagnostic {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code is required");
        }
        code = code.trim();
        stage = Objects.requireNonNull(stage, "stage");
        recordedAt = Objects.requireNonNull(recordedAt, "recordedAt");
    }
}
