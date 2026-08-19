package top.egon.cola.component.gateway.engine.http.service;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicLong;

public final class GatewayDataBufferTestSupport {

    private static final DefaultDataBufferFactory BUFFER_FACTORY =
            DefaultDataBufferFactory.sharedInstance;

    private GatewayDataBufferTestSupport() {
    }

    static DataBuffer buffer(String value) {
        return buffer(value.getBytes(StandardCharsets.UTF_8));
    }

    static DataBuffer buffer(byte[] value) {
        return BUFFER_FACTORY.wrap(value);
    }

    public static Flux<DataBuffer> body(String... chunks) {
        return Flux.fromArray(chunks)
                .map(GatewayDataBufferTestSupport::buffer);
    }

    static Flux<DataBuffer> body(byte[]... chunks) {
        return Flux.fromArray(chunks)
                .map(GatewayDataBufferTestSupport::buffer);
    }

    public static byte[] join(Flux<DataBuffer> body, int maxBytes) {
        DataBuffer joined = DataBufferUtils.join(body, maxBytes).block();
        if (joined == null) {
            return new byte[0];
        }
        try {
            byte[] bytes = new byte[joined.readableByteCount()];
            joined.read(bytes);
            return bytes;
        } finally {
            DataBufferUtils.release(joined);
        }
    }

    public static String joinUtf8(Flux<DataBuffer> body, int maxBytes) {
        return new String(join(body, maxBytes), StandardCharsets.UTF_8);
    }

    static String sha256(Flux<DataBuffer> body, long maxBytes) {
        return sha256Mono(body, maxBytes).block();
    }

    static Mono<String> sha256Mono(
            Flux<DataBuffer> body,
            long maxBytes) {
        return Mono.defer(() -> {
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException impossible) {
                return Mono.error(new IllegalStateException(impossible));
            }
            AtomicLong received = new AtomicLong();
            return body.doOnNext(buffer -> {
                try {
                    long total = received.addAndGet(
                            buffer.readableByteCount()
                    );
                    if (total > maxBytes) {
                        throw new AssertionError(
                                "test body exceeded bounded checksum"
                        );
                    }
                    try (DataBuffer.ByteBufferIterator byteBuffers =
                                 buffer.readableByteBuffers()) {
                        byteBuffers.forEachRemaining(digest::update);
                    }
                } finally {
                    DataBufferUtils.release(buffer);
                }
            }).then(Mono.fromSupplier(() ->
                    HexFormat.of().formatHex(digest.digest())
            ));
        });
    }
}
