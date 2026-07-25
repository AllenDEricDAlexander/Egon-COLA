package top.egon.cola.component.gateway.admin.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;

import java.io.IOException;
import java.util.Objects;

public final class GatewayCallEventCodec {

    public static final int MAX_PAYLOAD_BYTES = 64 * 1024;

    private final ObjectMapper mapper;

    public GatewayCallEventCodec(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public GatewayCallEventV1 decode(byte[] payload) {
        if (payload == null || payload.length == 0
                || payload.length > MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException(
                    "invalid gateway call event payload size"
            );
        }
        try {
            GatewayCallEventV1 event = mapper.readValue(
                    payload,
                    GatewayCallEventV1.class
            );
            if (!"v1".equals(event.eventSchemaVersion())) {
                throw new IllegalArgumentException(
                        "unsupported event schema version"
                );
            }
            return event;
        } catch (IOException failure) {
            throw new IllegalArgumentException(
                    "invalid gateway call event JSON",
                    failure
            );
        }
    }
}
