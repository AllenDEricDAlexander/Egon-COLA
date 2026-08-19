package top.egon.cola.component.gateway.engine.common.transport.service;

import top.egon.cola.component.gateway.engine.http.service.GatewayOutboundHttpResponse;
import top.egon.cola.component.gateway.engine.http.service.ReactorNettyHttpUpstreamAdapter;

import top.egon.cola.component.gateway.engine.http.websocket.service.GatewayWebSocketObserver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteProfile;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportResponseMode;
import top.egon.cola.component.gateway.core.transport.EffectiveGatewayTransportPolicy;
import top.egon.cola.component.gateway.engine.http.domain.GatewayHttpEngineProperties;
import top.egon.cola.component.gateway.engine.http.service.GatewayHttpServer;
import top.egon.cola.component.gateway.engine.http.domain.GatewayRequestBodyTooLargeException;
import top.egon.cola.component.gateway.engine.http.proxy.domain.GatewayHttpProxyContext;
import top.egon.cola.component.gateway.engine.http.proxy.service.StreamingHttpProxyStrategy;
import top.egon.cola.component.gateway.engine.common.transport.fixture.StreamingHttpTestUpstream;
import top.egon.cola.component.gateway.engine.common.transport.fixture.WebSocketTestUpstream;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketCloseStatus;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketFrame;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketFrameType;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketHandshakeResult;
import top.egon.cola.component.gateway.engine.http.websocket.service.GatewayWebSocketPeer;
import top.egon.cola.component.gateway.engine.http.websocket.service.GatewayWebSocketProxy;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketProxyContext;
import top.egon.cola.component.gateway.engine.http.websocket.adapter.ReactorNettyWebSocketUpstreamAdapter;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayOpenAiTransportComponentTest {

    private static final DefaultDataBufferFactory BUFFER_FACTORY =
            DefaultDataBufferFactory.sharedInstance;

    private StreamingHttpTestUpstream upstream;

    private ReactorNettyHttpUpstreamAdapter adapter;

    private StreamingHttpProxyStrategy strategy;

    @BeforeEach
    void setUp() {
        upstream = new StreamingHttpTestUpstream();
        adapter = new ReactorNettyHttpUpstreamAdapter(
                4,
                4,
                Duration.ofSeconds(5)
        );
        strategy = new StreamingHttpProxyStrategy();
    }

    @AfterEach
    void tearDown() {
        adapter.close();
        upstream.close();
    }

    @Test
    void preservesJsonBytesAndOpenAiEndToEndHeaders() {
        String json = "{ \"model\" : \"gpt-未来\",\n"
                + "  \"unknown\" : { \"z\": 1, \"a\": false } }";
        Map<String, List<String>> headers = Map.ofEntries(
                Map.entry(
                        "Content-Type",
                        List.of("application/json; charset=utf-8")
                ),
                Map.entry("Authorization", List.of("Bearer transparent")),
                Map.entry("OpenAI-Organization", List.of("org-test")),
                Map.entry("OpenAI-Project", List.of("project-test")),
                Map.entry("Idempotency-Key", List.of("idem-test")),
                Map.entry(
                        "Traceparent",
                        List.of(
                                "00-4bf92f3577b34da6a3ce929d0e0e4736"
                                        + "-00f067aa0ba902b7-01"
                        )
                ),
                Map.entry("Connection", List.of("keep-alive, X-Remove")),
                Map.entry("X-Remove", List.of("hop-by-hop"))
        );

        GatewayOutboundHttpResponse response = proxy(
                "/echo",
                "POST",
                headers,
                chunks(
                        json.substring(0, 17).getBytes(StandardCharsets.UTF_8),
                        json.substring(17).getBytes(StandardCharsets.UTF_8)
                ),
                policy(GatewayTransportResponseMode.AUTO_STREAM, 1024 * 1024)
        );

        assertArrayEquals(
                json.getBytes(StandardCharsets.UTF_8),
                bytes(response.body())
        );
        Map<String, List<String>> forwarded = upstream.lastHeaders();
        assertEquals(
                List.of("application/json; charset=utf-8"),
                forwarded.get("content-type")
        );
        assertEquals(
                List.of("Bearer transparent"),
                forwarded.get("authorization")
        );
        assertEquals(List.of("org-test"), forwarded.get(
                "openai-organization"
        ));
        assertEquals(List.of("project-test"), forwarded.get(
                "openai-project"
        ));
        assertEquals(List.of("idem-test"), forwarded.get(
                "idempotency-key"
        ));
        assertTrue(forwarded.containsKey("traceparent"));
        assertFalse(forwarded.containsKey("connection"));
        assertFalse(forwarded.containsKey("x-remove"));
        assertEquals(1, upstream.invocations("/echo"));
    }

    @Test
    void streamsMultipartLargerThanTwoMibWithoutOneLargeBuffer() {
        String boundary = "egon-boundary-7MA4YWxk";
        byte[] prefix = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=purpose\r\n\r\n"
                + "transcription\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=file; "
                + "filename=voice.wav\r\n"
                + "Content-Type: audio/wav\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8);
        byte[] suffix = ("\r\n--" + boundary + "--\r\n")
                .getBytes(StandardCharsets.UTF_8);
        int fileBytes = 2 * 1024 * 1024 + 513;
        long contentLength = prefix.length + (long) fileBytes + suffix.length;
        StreamingHttpTestUpstream.UploadProbe probe =
                upstream.expectUpload(prefix.length, fileBytes);
        GeneratedUpload upload = upload(prefix, suffix, fileBytes);

        GatewayOutboundHttpResponse response = proxy(
                "/upload",
                "POST",
                Map.of(
                        "Content-Type",
                        List.of("multipart/form-data; boundary=" + boundary),
                        "Content-Length",
                        List.of(Long.toString(contentLength)),
                        "Authorization",
                        List.of("Bearer transparent")
                ),
                upload.body(),
                policy(
                        GatewayTransportResponseMode.STANDARD,
                        contentLength + 1024
                )
        );
        bytes(response.body());

        StreamingHttpTestUpstream.UploadSnapshot snapshot = probe.snapshot();
        assertEquals(contentLength, snapshot.receivedBytes());
        assertTrue(snapshot.prefix().contains("name=purpose"));
        assertTrue(snapshot.prefix().contains("transcription"));
        assertTrue(snapshot.prefix().contains("filename=voice.wav"));
        assertEquals(upload.fileSha256(), snapshot.fileSha256());
        assertEquals(upload.bodySha256(), snapshot.bodySha256());
        assertTrue(snapshot.chunks() > 128);
        assertTrue(snapshot.largestChunkBytes() < 256 * 1024);
        assertEquals(1, upstream.invocations("/upload"));
    }

    @Test
    void rejectsDeclaredOversizeBeforeOpeningTheUpstream() {
        GatewayRequestBodyTooLargeException failure = assertThrows(
                GatewayRequestBodyTooLargeException.class,
                () -> strategy.proxy(new GatewayHttpProxyContext(
                        adapter,
                        upstream.provider(),
                        "POST",
                        "/upload",
                        Map.of("Content-Length", List.of("2048")),
                        Flux.empty(),
                        policy(GatewayTransportResponseMode.STANDARD, 1024)
                ))
        );

        assertTrue(failure.getMessage().contains("content-length"));
        assertEquals(0, upstream.invocations("/upload"));
    }

    @Test
    void preservesLargeInvalidUtf8BinaryResponseAndMetadata() {
        GatewayOutboundHttpResponse response = proxy(
                "/binary",
                "GET",
                Map.of("Authorization", List.of("Bearer transparent")),
                Flux.empty(),
                policy(
                        GatewayTransportResponseMode.BINARY_STREAM,
                        1024
                )
        );

        assertEquals(
                StreamingHttpTestUpstream.binarySha256(),
                sha256(response.body())
        );
        assertEquals(
                List.of("audio/mpeg"),
                response.headers().get("content-type")
        );
        assertEquals(
                List.of("attachment; filename=voice.mp3"),
                response.headers().get("content-disposition")
        );
        assertEquals(
                List.of("identity"),
                response.headers().get("content-encoding")
        );
    }

    @Test
    void flushesSseBeforeCompletionAndAppliesNoBufferHeaders() {
        EffectiveGatewayTransportPolicy policy = policy(
                GatewayTransportResponseMode.AUTO_STREAM,
                1024
        );
        GatewayHttpServer gateway = new GatewayHttpServer(
                properties(),
                (zone, request) -> strategy.proxy(
                        new GatewayHttpProxyContext(
                                adapter,
                                upstream.provider(),
                                request.method(),
                                request.uri(),
                                request.headers(),
                                request.body(),
                                policy
                        )
                )
        );
        gateway.start();
        AtomicLong firstBodyNanos = new AtomicLong();
        AtomicReference<Map<String, String>> headers =
                new AtomicReference<>();
        long started = System.nanoTime();
        try {
            String body = HttpClient.create()
                    .get()
                    .uri("http://127.0.0.1:"
                            + gateway.publicPort()
                            + "/sse")
                    .response((response, inbound) -> {
                        headers.set(Map.of(
                                "cache-control",
                                String.valueOf(response.responseHeaders().get(
                                        "cache-control"
                                )),
                                "x-accel-buffering",
                                String.valueOf(response.responseHeaders().get(
                                        "x-accel-buffering"
                                )),
                                "content-length",
                                String.valueOf(response.responseHeaders().get(
                                        "content-length"
                                ))
                        ));
                        return inbound.map(buffer -> {
                                    firstBodyNanos.compareAndSet(
                                            0,
                                            System.nanoTime()
                                    );
                                    byte[] chunk = new byte[
                                            buffer.readableBytes()
                                    ];
                                    buffer.readBytes(chunk);
                                    return chunk;
                                })
                                .reduce(
                                        new ByteArrayOutputStream(),
                                        (output, chunk) -> {
                                            output.writeBytes(chunk);
                                            return output;
                                        }
                                )
                                .map(output -> output.toString(
                                        StandardCharsets.UTF_8
                                ));
                    })
                    .single()
                    .block(Duration.ofSeconds(3));

            long firstMillis = Duration.ofNanos(
                    firstBodyNanos.get() - started
            ).toMillis();
            assertEquals(
                    "data: first\n\ndata: second\n\ndata: [DONE]\n\n",
                    body
            );
            assertTrue(firstMillis < 550, "firstMillis=" + firstMillis);
            assertEquals("no-cache, no-transform", headers.get().get(
                    "cache-control"
            ));
            assertEquals("no", headers.get().get("x-accel-buffering"));
            assertEquals("null", headers.get().get("content-length"));
        } finally {
            gateway.close();
        }
    }

    @Test
    void proxiesRealtimeFramesAndDisablesWebSocketExtensions()
            throws Exception {
        try (WebSocketTestUpstream websocket =
                     new WebSocketTestUpstream()) {
            GatewayWebSocketProxy proxy = new GatewayWebSocketProxy(
                    new ReactorNettyWebSocketUpstreamAdapter(
                            HttpClient.create()
                    )
            );
            GatewayWebSocketProxyContext context =
                    new GatewayWebSocketProxyContext(
                            websocket.provider(),
                            "/v1/realtime?model=gpt-realtime",
                            Map.of(
                                    "Authorization",
                                    List.of("Bearer transparent"),
                                    "Origin",
                                    List.of("https://client.example"),
                                    "Sec-WebSocket-Extensions",
                                    List.of("permessage-deflate")
                            ),
                            List.of("realtime", "fallback"),
                            websocketPolicy(),
                            GatewayCommitGuard.websocket(),
                                    top.egon.cola.component.gateway.engine.http.websocket.service
                                    .GatewayWebSocketObserver.noop()
                    );
            GatewayWebSocketHandshakeResult.Accepted accepted =
                    assertInstanceOf(
                            GatewayWebSocketHandshakeResult.Accepted.class,
                            proxy.prepare(context).block(
                                    Duration.ofSeconds(2)
                            )
                    );
            TestWebSocketPeer downstream = new TestWebSocketPeer();

            reactor.core.Disposable bridge = proxy.bridge(
                    accepted.session(),
                    downstream
            ).subscribe();
            try {
                assertTrue(downstream.framesReceived.await(
                        1,
                        TimeUnit.SECONDS
                ));
            } finally {
                bridge.dispose();
            }

            assertEquals(1, websocket.handshakes());
            assertEquals("Bearer transparent", websocket.authorization());
            assertEquals("https://client.example", websocket.origin());
            assertNull(websocket.extensions());
            assertEquals(
                    "realtime",
                    accepted.session().selectedSubprotocol()
            );
            assertTrue(downstream.received.contains(
                    GatewayWebSocketFrameType.TEXT
            ));
            assertTrue(downstream.received.contains(
                    GatewayWebSocketFrameType.BINARY
            ));
            assertTrue(downstream.received.contains(
                    GatewayWebSocketFrameType.PING
            ));
            assertTrue(downstream.received.contains(
                    GatewayWebSocketFrameType.PONG
            ));
        }
    }

    private GatewayOutboundHttpResponse proxy(
            String path,
            String method,
            Map<String, List<String>> headers,
            Flux<DataBuffer> body,
            EffectiveGatewayTransportPolicy policy) {
        return strategy.proxy(new GatewayHttpProxyContext(
                adapter,
                upstream.provider(),
                method,
                path,
                headers,
                body,
                policy
        )).block(Duration.ofSeconds(3));
    }

    private EffectiveGatewayTransportPolicy policy(
            GatewayTransportResponseMode responseMode,
            long maxRequestBytes) {
        return new EffectiveGatewayTransportPolicy(
                GatewayRouteProfile.OPENAI_HTTP,
                GatewayTransportProtocol.HTTP,
                GatewayRequestBodyMode.STREAMING,
                responseMode,
                maxRequestBytes,
                OptionalLong.empty(),
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                Duration.ofSeconds(2),
                Optional.of(Duration.ofSeconds(5)),
                Optional.empty(),
                OptionalLong.empty(),
                false,
                false,
                true
        );
    }

    private GatewayHttpEngineProperties properties() {
        return new GatewayHttpEngineProperties(
                new GatewayHttpEngineProperties.Listener(
                        true,
                        "127.0.0.1",
                        0
                ),
                new GatewayHttpEngineProperties.Listener(
                        false,
                        "127.0.0.1",
                        0
                ),
                64,
                8192,
                1024,
                Duration.ofSeconds(30),
                Duration.ofSeconds(2),
                4,
                4
        );
    }

    private EffectiveGatewayTransportPolicy websocketPolicy() {
        return new EffectiveGatewayTransportPolicy(
                GatewayRouteProfile.OPENAI_HTTP,
                GatewayTransportProtocol.WEBSOCKET,
                GatewayRequestBodyMode.STREAMING,
                GatewayTransportResponseMode.AUTO_STREAM,
                1024,
                OptionalLong.empty(),
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                Duration.ofSeconds(2),
                Optional.empty(),
                Optional.of(Duration.ofSeconds(2)),
                OptionalLong.of(64 * 1024),
                false,
                false,
                true
        );
    }

    private Flux<DataBuffer> chunks(byte[]... chunks) {
        return Flux.fromArray(chunks).map(BUFFER_FACTORY::wrap);
    }

    private GeneratedUpload upload(
            byte[] prefix,
            byte[] suffix,
            int fileBytes) {
        MessageDigest fileDigest = digest();
        MessageDigest bodyDigest = digest();
        bodyDigest.update(prefix);
        int chunkBytes = 8192;
        int chunks = (fileBytes + chunkBytes - 1) / chunkBytes;
        for (int index = 0; index < chunks; index++) {
            int offset = index * chunkBytes;
            byte[] bytes = StreamingHttpTestUpstream.binaryChunk(
                    offset,
                    Math.min(chunkBytes, fileBytes - offset)
            );
            fileDigest.update(bytes);
            bodyDigest.update(bytes);
        }
        bodyDigest.update(suffix);
        Flux<DataBuffer> file = Flux.range(0, chunks).map(index -> {
            int offset = index * chunkBytes;
            byte[] bytes = StreamingHttpTestUpstream.binaryChunk(
                    offset,
                    Math.min(chunkBytes, fileBytes - offset)
            );
            return BUFFER_FACTORY.wrap(bytes);
        });
        Flux<DataBuffer> body = Flux.concat(
                Flux.just(BUFFER_FACTORY.wrap(prefix)),
                file,
                Flux.just(BUFFER_FACTORY.wrap(suffix))
        );
        return new GeneratedUpload(
                body,
                HexFormat.of().formatHex(fileDigest.digest()),
                HexFormat.of().formatHex(bodyDigest.digest())
        );
    }

    private byte[] bytes(Flux<DataBuffer> body) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        body.doOnNext(buffer -> {
            try {
                byte[] chunk = new byte[buffer.readableByteCount()];
                buffer.read(chunk);
                output.writeBytes(chunk);
            } finally {
                DataBufferUtils.release(buffer);
            }
        }).blockLast(Duration.ofSeconds(3));
        return output.toByteArray();
    }

    private String sha256(Flux<DataBuffer> body) {
        MessageDigest digest = digest();
        body.doOnNext(buffer -> {
            try (DataBuffer.ByteBufferIterator buffers =
                         buffer.readableByteBuffers()) {
                buffers.forEachRemaining(digest::update);
            } finally {
                DataBufferUtils.release(buffer);
            }
        }).blockLast(Duration.ofSeconds(3));
        return HexFormat.of().formatHex(digest.digest());
    }

    private MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private record GeneratedUpload(
            Flux<DataBuffer> body,
            String fileSha256,
            String bodySha256
    ) {
    }

    private static final class TestWebSocketPeer
            implements GatewayWebSocketPeer {

        private final List<GatewayWebSocketFrameType> received =
                new CopyOnWriteArrayList<>();

        private final CountDownLatch framesReceived = new CountDownLatch(4);

        private volatile boolean disposed;

        @Override
        public Flux<GatewayWebSocketFrame> receive() {
            return Flux.just(
                    frame(GatewayWebSocketFrameType.TEXT, "hello"),
                    frame(
                            GatewayWebSocketFrameType.BINARY,
                            new byte[]{0, (byte) 0xff, 1}
                    ),
                    frame(GatewayWebSocketFrameType.PING, "ping"),
                    frame(GatewayWebSocketFrameType.PONG, "pong")
            ).concatWith(Flux.never());
        }

        @Override
        public Mono<Void> send(Flux<GatewayWebSocketFrame> frames) {
            return frames.doOnNext(frame -> {
                received.add(frame.type());
                frame.release();
                framesReceived.countDown();
            }).then();
        }

        @Override
        public Mono<Void> sendClose(GatewayWebSocketCloseStatus status) {
            return Mono.empty();
        }

        @Override
        public void dispose() {
            disposed = true;
        }

        @Override
        public boolean disposed() {
            return disposed;
        }

        private static GatewayWebSocketFrame frame(
                GatewayWebSocketFrameType type,
                String value) {
            return frame(type, value.getBytes(StandardCharsets.UTF_8));
        }

        private static GatewayWebSocketFrame frame(
                GatewayWebSocketFrameType type,
                byte[] value) {
            return GatewayWebSocketFrame.data(
                    type,
                    true,
                    BUFFER_FACTORY.wrap(value)
            );
        }
    }
}
