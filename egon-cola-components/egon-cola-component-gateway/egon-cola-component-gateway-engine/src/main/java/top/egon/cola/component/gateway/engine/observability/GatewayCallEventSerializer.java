package top.egon.cola.component.gateway.engine.observability;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;

import java.io.IOException;

public final class GatewayCallEventSerializer {

    public static final int MAX_PAYLOAD_BYTES = 64 * 1024;

    private final ObjectMapper mapper = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .build();

    public byte[] serialize(GatewayCallEventV1 event) {
        try {
            byte[] payload = mapper.writeValueAsBytes(event);
            if (payload.length > MAX_PAYLOAD_BYTES) {
                throw new PayloadTooLargeException(payload.length);
            }
            return payload;
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "gateway call event serialization failed",
                    failure
            );
        }
    }

    public GatewayCallEventV1 deserialize(byte[] payload) {
        if (payload == null || payload.length > MAX_PAYLOAD_BYTES) {
            throw new PayloadTooLargeException(
                    payload == null ? 0 : payload.length
            );
        }
        try {
            return mapper.readValue(payload, GatewayCallEventV1.class);
        } catch (IOException failure) {
            throw new IllegalArgumentException(
                    "invalid gateway call event",
                    failure
            );
        }
    }

    public static final class PayloadTooLargeException
            extends IllegalArgumentException {

        public PayloadTooLargeException(int bytes) {
            super("gateway call event exceeds "
                    + MAX_PAYLOAD_BYTES
                    + " bytes: "
                    + bytes);
        }
    }
}
