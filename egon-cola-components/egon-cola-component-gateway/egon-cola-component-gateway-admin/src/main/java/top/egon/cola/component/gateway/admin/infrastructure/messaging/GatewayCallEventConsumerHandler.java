package top.egon.cola.component.gateway.admin.infrastructure.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.application.observability.GatewayCallEventIngestService;
import top.egon.cola.component.gateway.admin.application.observability.GatewayObservabilityStore;

import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Objects;

public final class GatewayCallEventConsumerHandler {

    private final GatewayCallEventCodec codec;

    private final GatewayCallEventIngestService ingestService;

    private final Clock clock;

    public GatewayCallEventConsumerHandler(
            GatewayCallEventCodec codec,
            GatewayCallEventIngestService ingestService,
            Clock clock) {
        this.codec = Objects.requireNonNull(codec, "codec");
        this.ingestService = Objects.requireNonNull(
                ingestService,
                "ingestService"
        );
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Returns only after the projection or poison record transaction commits.
     */
    public Result handle(ConsumerRecord<String, byte[]> record) {
        try {
            boolean inserted = ingestService.ingest(codec.decode(
                    record.value()
            ));
            return inserted ? Result.PROJECTED : Result.DUPLICATE;
        } catch (IllegalArgumentException poison) {
            byte[] payload = record.value() == null
                    ? new byte[0]
                    : record.value();
            ingestService.poison(
                    new GatewayObservabilityStore.ConsumeFailure(
                            UuidV7.simpleString(),
                            record.topic(),
                            record.partition(),
                            record.offset(),
                            header(record, "event-id"),
                            "GATEWAY_CALL_EVENT_INVALID",
                            bounded(poison.getMessage()),
                            sha256(payload),
                            payload.length,
                            clock.instant()
                    )
            );
            return Result.POISON_RECORDED;
        }
    }

    private String header(
            ConsumerRecord<String, byte[]> record,
            String name) {
        org.apache.kafka.common.header.Header header =
                record.headers().lastHeader(name);
        return header == null
                ? null
                : new String(
                        header.value(),
                        java.nio.charset.StandardCharsets.UTF_8
                );
    }

    private String sha256(byte[] payload) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(payload)
            );
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private String bounded(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 1024 ? value : value.substring(0, 1024);
    }

    public enum Result {
        PROJECTED,
        DUPLICATE,
        POISON_RECORDED
    }
}
