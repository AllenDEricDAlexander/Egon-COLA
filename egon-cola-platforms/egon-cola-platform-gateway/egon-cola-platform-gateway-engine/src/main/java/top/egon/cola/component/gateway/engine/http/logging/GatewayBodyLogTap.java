package top.egon.cola.component.gateway.engine.http.logging;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Optional body-log decorator that never owns or consumes a DataBuffer.
 */
public final class GatewayBodyLogTap {

    public static final int DEFAULT_SAMPLE_BYTES = 8 * 1024;

    public static final int MAX_SAMPLE_BYTES = 64 * 1024;

    private GatewayBodyLogTap() {
    }

    public static Flux<DataBuffer> tap(
            Flux<DataBuffer> source,
            boolean enabled,
            String contentType,
            GatewayBodyLogDirection direction,
            int requestedSampleBytes,
            Consumer<GatewayBodyLogEvent> observer) {
        Objects.requireNonNull(source, "source");
        if (!enabled) {
            return source;
        }
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(observer, "observer");
        int sampleLimit = Math.min(
                requirePositive(requestedSampleBytes),
                MAX_SAMPLE_BYTES
        );
        String normalizedContentType = normalize(contentType);
        boolean metadataOnly = direction
                == GatewayBodyLogDirection.WEBSOCKET
                || metadataOnly(normalizedContentType);
        return Flux.defer(() -> {
            AtomicLong totalBytes = new AtomicLong();
            ByteArrayOutputStream sample = new ByteArrayOutputStream(
                    metadataOnly ? 0 : Math.min(sampleLimit, 1024)
            );
            return source.doOnNext(buffer -> {
                totalBytes.addAndGet(buffer.readableByteCount());
                if (!metadataOnly && sample.size() < sampleLimit) {
                    copySample(buffer, sample, sampleLimit);
                }
            }).doFinally(ignored -> notifyObserver(
                    observer,
                    new GatewayBodyLogEvent(
                            direction,
                            normalizedContentType,
                            totalBytes.get(),
                            metadataOnly,
                            sample.toByteArray()
                    )
            ));
        });
    }

    public static List<String> safeHeaderNames(
            Iterable<String> headerNames) {
        Objects.requireNonNull(headerNames, "headerNames");
        List<String> safe = new ArrayList<>();
        for (String name : headerNames) {
            if (name != null && !credentialHeader(name)) {
                safe.add(name);
            }
        }
        return List.copyOf(safe);
    }

    private static void copySample(
            DataBuffer buffer,
            ByteArrayOutputStream sample,
            int sampleLimit) {
        int remaining = sampleLimit - sample.size();
        try (DataBuffer.ByteBufferIterator buffers =
                     buffer.readableByteBuffers()) {
            while (buffers.hasNext() && remaining > 0) {
                ByteBuffer bytes = buffers.next().duplicate();
                int count = Math.min(remaining, bytes.remaining());
                byte[] chunk = new byte[count];
                bytes.get(chunk);
                sample.writeBytes(chunk);
                remaining -= count;
            }
        }
    }

    private static void notifyObserver(
            Consumer<GatewayBodyLogEvent> observer,
            GatewayBodyLogEvent event) {
        try {
            observer.accept(event);
        } catch (RuntimeException ignored) {
            // Logging is passive and must not change transport behavior.
        }
    }

    private static boolean metadataOnly(String contentType) {
        return contentType.startsWith("multipart/")
                || contentType.startsWith("image/")
                || contentType.startsWith("audio/")
                || "application/octet-stream".equals(contentType);
    }

    private static boolean credentialHeader(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return "authorization".equals(normalized)
                || "proxy-authorization".equals(normalized)
                || "cookie".equals(normalized)
                || "set-cookie".equals(normalized)
                || normalized.contains("api-key");
    }

    private static String normalize(String contentType) {
        if (contentType == null) {
            return "";
        }
        int parameters = contentType.indexOf(';');
        String mediaType = parameters < 0
                ? contentType
                : contentType.substring(0, parameters);
        return mediaType.trim().toLowerCase(Locale.ROOT);
    }

    private static int requirePositive(int value) {
        if (value < 1) {
            throw new IllegalArgumentException(
                    "requestedSampleBytes must be positive"
            );
        }
        return value;
    }
}
