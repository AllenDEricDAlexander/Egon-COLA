package top.egon.cola.component.gateway.engine.http.logging;

import java.util.Objects;

public record GatewayBodyLogEvent(
        GatewayBodyLogDirection direction,
        String contentType,
        long totalBytes,
        boolean metadataOnly,
        byte[] sample
) {

    public GatewayBodyLogEvent {
        direction = Objects.requireNonNull(direction, "direction");
        contentType = contentType == null ? "" : contentType;
        if (totalBytes < 0) {
            throw new IllegalArgumentException(
                    "totalBytes must be non-negative"
            );
        }
        sample = sample == null ? new byte[0] : sample.clone();
    }

    @Override
    public byte[] sample() {
        return sample.clone();
    }
}
