package top.egon.cola.component.gateway.engine.transport.fixture;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import top.egon.cola.component.gateway.core.provider.ProviderHealthState;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderRegistryState;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-process byte-oriented HTTP upstream used by transport component tests.
 */
public final class StreamingHttpTestUpstream implements AutoCloseable {

    public static final int BINARY_BYTES = 4 * 1024 * 1024 + 257;

    private static final int BINARY_CHUNK_BYTES = 8192;

    private final ConcurrentHashMap<String, AtomicInteger> invocations =
            new ConcurrentHashMap<>();

    private final AtomicReference<Map<String, List<String>>> lastHeaders =
            new AtomicReference<>(Map.of());

    private final AtomicReference<UploadProbe> uploadProbe =
            new AtomicReference<>();

    private final CountDownLatch cancelObserved = new CountDownLatch(1);

    private final DisposableServer server;

    public StreamingHttpTestUpstream() {
        server = HttpServer.create()
                .host("127.0.0.1")
                .port(0)
                .handle((request, response) -> {
                    String path = request.uri();
                    int query = path.indexOf('?');
                    if (query >= 0) {
                        path = path.substring(0, query);
                    }
                    invocations.computeIfAbsent(
                            path,
                            ignored -> new AtomicInteger()
                    ).incrementAndGet();
                    lastHeaders.set(headers(request.requestHeaders()));
                    return switch (path) {
                        case "/echo" -> response
                                .header(
                                        "Content-Type",
                                        header(
                                                request.requestHeaders(),
                                                "Content-Type",
                                                "application/octet-stream"
                                        )
                                )
                                .send(request.receive().retain());
                        case "/upload" -> upload(request.receive(), response);
                        case "/sse" -> response
                                .header("Content-Type", "text/event-stream")
                                .header("Cache-Control", "private")
                                .header("Content-Length", "41")
                                .sendString(Flux.concat(
                                        Mono.just("data: first\n\n"),
                                        Mono.delay(Duration.ofMillis(600))
                                                .map(ignored ->
                                                        "data: second\n\n"),
                                        Mono.delay(Duration.ofMillis(100))
                                                .map(ignored ->
                                                        "data: [DONE]\n\n")
                                ));
                        case "/binary" -> response
                                .header("Content-Type", "audio/mpeg")
                                .header(
                                        "Content-Disposition",
                                        "attachment; filename=voice.mp3"
                                )
                                .header("Content-Encoding", "identity")
                                .send(binary(response));
                        case "/slow-headers" -> Mono.delay(
                                        Duration.ofMillis(300)
                                )
                                .then(response.sendString(Mono.just("late"))
                                        .then());
                        case "/idle-response" -> response.sendString(
                                Flux.concat(
                                        Mono.just("first"),
                                        Mono.delay(Duration.ofMillis(300))
                                                .map(ignored -> "second")
                                )
                        );
                        case "/total" -> response.sendString(
                                Flux.interval(Duration.ofMillis(20))
                                        .take(50)
                                        .map(index -> "chunk-" + index + "\n")
                        );
                        case "/cancel" -> response.sendString(
                                Flux.interval(Duration.ofMillis(20))
                                        .map(index -> "chunk-" + index + "\n")
                                        .doOnCancel(cancelObserved::countDown)
                        );
                        case "/status-503" -> response.status(503)
                                .sendString(Mono.just("unavailable"));
                        default -> response.status(404).send();
                    };
                })
                .bindNow();
    }

    public int port() {
        return server.port();
    }

    public ProviderInstance provider() {
        return new ProviderInstance(
                new ProviderServiceKey(
                        "test-biz",
                        "test-app",
                        "test",
                        "default",
                        ProviderProtocolType.HTTP,
                        "openai-component-upstream",
                        "default",
                        "v1",
                        "http"
                ),
                "provider-1",
                "lease-1",
                "127.0.0.1",
                port(),
                false,
                Map.of(),
                Instant.now().plusSeconds(60),
                ProviderRegistryState.REGISTERED,
                ProviderHealthState.HEALTHY,
                ProviderHealthState.HEALTHY
        );
    }

    public int invocations(String path) {
        AtomicInteger count = invocations.get(path);
        return count == null ? 0 : count.get();
    }

    public Map<String, List<String>> lastHeaders() {
        return lastHeaders.get();
    }

    public UploadProbe expectUpload(long prefixBytes, long fileBytes) {
        UploadProbe probe = new UploadProbe(prefixBytes, fileBytes);
        uploadProbe.set(probe);
        return probe;
    }

    public CountDownLatch cancelObserved() {
        return cancelObserved;
    }

    public static byte[] binaryChunk(int offset, int length) {
        byte[] chunk = new byte[length];
        for (int index = 0; index < length; index++) {
            int absolute = offset + index;
            chunk[index] = absolute % 257 == 0
                    ? (byte) 0xff
                    : (byte) (absolute % 251);
        }
        return chunk;
    }

    public static String binarySha256() {
        MessageDigest digest = digest();
        for (int offset = 0; offset < BINARY_BYTES;
             offset += BINARY_CHUNK_BYTES) {
            digest.update(binaryChunk(
                    offset,
                    Math.min(BINARY_CHUNK_BYTES, BINARY_BYTES - offset)
            ));
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    @Override
    public void close() {
        server.disposeNow();
    }

    private org.reactivestreams.Publisher<Void> upload(
            Flux<ByteBuf> body,
            reactor.netty.http.server.HttpServerResponse response) {
        UploadProbe probe = uploadProbe.get();
        if (probe == null) {
            return response.status(500).send();
        }
        return body.doOnNext(probe::accept)
                .then(Mono.defer(() -> {
                    probe.complete();
                    return response.status(204).send().then();
                }));
    }

    private Flux<ByteBuf> binary(
            reactor.netty.http.server.HttpServerResponse response) {
        int chunks = (BINARY_BYTES + BINARY_CHUNK_BYTES - 1)
                / BINARY_CHUNK_BYTES;
        return Flux.range(0, chunks).map(index -> {
            int offset = index * BINARY_CHUNK_BYTES;
            int length = Math.min(
                    BINARY_CHUNK_BYTES,
                    BINARY_BYTES - offset
            );
            return response.alloc().buffer(length)
                    .writeBytes(binaryChunk(offset, length));
        });
    }

    private Map<String, List<String>> headers(HttpHeaders source) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        source.forEach(entry -> result.computeIfAbsent(
                entry.getKey().toLowerCase(),
                ignored -> new ArrayList<>()
        ).add(entry.getValue()));
        result.replaceAll((ignored, values) -> List.copyOf(values));
        return Map.copyOf(result);
    }

    private String header(
            HttpHeaders headers,
            String name,
            String fallback) {
        String value = headers.get(name);
        return value == null ? fallback : value;
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public static final class UploadProbe {

        private final long prefixBytes;

        private final long fileBytes;

        private final MessageDigest bodyDigest = digest();

        private final MessageDigest fileDigest = digest();

        private final ByteArrayOutputStream prefix =
                new ByteArrayOutputStream();

        private long received;

        private int chunks;

        private int largestChunk;

        private volatile UploadSnapshot snapshot;

        private UploadProbe(long prefixBytes, long fileBytes) {
            this.prefixBytes = prefixBytes;
            this.fileBytes = fileBytes;
        }

        private void accept(ByteBuf buffer) {
            int readable = buffer.readableBytes();
            byte[] bytes = new byte[readable];
            buffer.getBytes(buffer.readerIndex(), bytes);
            bodyDigest.update(bytes);
            long start = received;
            for (int index = 0; index < bytes.length; index++) {
                long position = start + index;
                if (position < prefixBytes) {
                    prefix.write(bytes[index]);
                } else if (position < prefixBytes + fileBytes) {
                    fileDigest.update(bytes[index]);
                }
            }
            received += readable;
            chunks++;
            largestChunk = Math.max(largestChunk, readable);
        }

        private void complete() {
            snapshot = new UploadSnapshot(
                    received,
                    chunks,
                    largestChunk,
                    new String(prefix.toByteArray(), StandardCharsets.UTF_8),
                    HexFormat.of().formatHex(fileDigest.digest()),
                    HexFormat.of().formatHex(bodyDigest.digest())
            );
        }

        public UploadSnapshot snapshot() {
            UploadSnapshot value = snapshot;
            if (value == null) {
                throw new IllegalStateException("upload is not complete");
            }
            return value;
        }
    }

    public record UploadSnapshot(
            long receivedBytes,
            int chunks,
            int largestChunkBytes,
            String prefix,
            String fileSha256,
            String bodySha256
    ) {
    }
}
