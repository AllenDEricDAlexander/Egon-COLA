package top.egon.cola.component.gateway.test.webflux;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.result.method.annotation
        .RequestMappingHandlerMapping;
import top.egon.cola.component.ddc.autoconfigure.DdcProperties;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.api.registry.DdcRegistrySubscription;
import top.egon.cola.component.ddc.service.registry.DdcServiceKeyFactory;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.gateway.contract.definition
        .GatewayDefinitionIdentity;
import top.egon.cola.component.gateway.contract.reporting
        .GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.provider.HttpProviderLeaseRuntime;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.discovery.http.WebFluxGatewayDefinitionContributor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = GatewayWebFluxHttpTestProviderApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(WebFluxHttpProviderContractTest.ProviderTestConfiguration.class)
class WebFluxHttpProviderContractTest {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mappings;

    @Autowired
    private GatewayReportingProperties reportingProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HttpProviderLeaseRuntime runtime;

    @Autowired
    private RecordingRegistry registry;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RealtimeWebSocketProbe webSocketProbe;

    @LocalServerPort
    private int port;

    @Test
    void reportsAnnotatedMonoEndpointAndAutoRegisters() {
        assertNotNull(runtime);
        assertEquals(1, registry.registrations.get());
        assertEquals(
                "gateway-test-http-provider",
                registry.registration.serviceKey().serviceName()
        );
        assertEquals(
                reportingProperties.getArtifactVersion(),
                registry.registration.serviceKey().version()
        );
        assertEquals(
                "webflux-http-provider-default",
                registry.registration.instanceId()
        );
        assertEquals(
                "zone-b",
                registry.registration.metadata().get("gateway.zone")
        );
        assertTrue(registry.registration.port() > 0);

        GatewayInterfaceDefinitionReport.Operation operation =
                new WebFluxGatewayDefinitionContributor(
                        mappings,
                        reportingProperties,
                        objectMapper
                ).discover().stream()
                        .flatMap(group -> group.interfaceGroup()
                                .operations().stream())
                        .filter(candidate -> "/test/items/{id}".equals(
                                candidate.attributes().get("path")
                        ))
                        .findFirst()
                        .orElseThrow();

        assertEquals("GET", operation.attributes().get("httpMethod"));
        assertEquals(
                "TRANSPARENT",
                operation.attributes().get("responseMode")
        );
        assertFalse((Boolean) operation.attributes().get("streaming"));
        assertEquals("SUPPORTED", operation.gatewaySupport());
        assertEquals("gateway-operation-response/v2",
                operation.responseSchema().get("x-egon-schema-model"));
        assertEquals("object", operation.responseSchema().get("type"));
        assertEquals(
                Set.of("id", "providerId", "framework"),
                ((Map<?, ?>) operation.responseSchema().get("properties"))
                        .keySet()
        );

        webTestClient.get()
                .uri("/test/items/item-1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("item-1")
                .jsonPath("$.providerId")
                .isEqualTo("webflux-http-provider-default")
                .jsonPath("$.framework").isEqualTo("webflux");

        webTestClient.get()
                .uri("/api/providers/request-1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.requestId").isEqualTo("request-1")
                .jsonPath("$.providerId")
                .isEqualTo("webflux-http-provider-default")
                .jsonPath("$.framework").isEqualTo("webflux");
    }

    @Test
    void streamsJsonSseUploadAndOpaqueBinaryWithoutDiscoveringWebSocket()
            throws Exception {
        byte[] json = ("{\"model\":\"gpt-test\",\"input\":["
                + "{\"type\":\"input_text\",\"text\":\"你好\"}]}")
                .getBytes(StandardCharsets.UTF_8);
        webTestClient.post()
                .uri("/test/transport/json")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(byte[].class).isEqualTo(json);

        byte[] firstSseBuffer = webTestClient.get()
                .uri("/test/transport/sse")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(
                        MediaType.TEXT_EVENT_STREAM
                )
                .expectHeader().cacheControl(CacheControl.noCache())
                .returnResult(byte[].class)
                .getResponseBody()
                .next()
                .block(Duration.ofSeconds(2));
        assertNotNull(firstSseBuffer);
        assertTrue(new String(firstSseBuffer, StandardCharsets.UTF_8)
                .contains("response.output_text.delta"));

        int chunkBytes = 8 * 1024;
        int chunkCount = 257;
        MessageDigest expectedDigest = MessageDigest.getInstance("SHA-256");
        for (int index = 0; index < chunkCount; index++) {
            expectedDigest.update(uploadChunk(index, chunkBytes));
        }
        webTestClient.post()
                .uri("/test/transport/upload")
                .contentType(MediaType.parseMediaType(
                        "multipart/form-data;boundary=gateway-test"
                ))
                .body(BodyInserters.fromDataBuffers(
                        reactor.core.publisher.Flux.range(0, chunkCount)
                                .map(index -> DefaultDataBufferFactory
                                        .sharedInstance.wrap(uploadChunk(
                                                index,
                                                chunkBytes
                                        )))
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.bytes")
                .isEqualTo((long) chunkBytes * chunkCount)
                .jsonPath("$.chunks").value(value -> assertTrue(
                        ((Number) value).intValue() > 1
                ))
                .jsonPath("$.maxChunkBytes").value(value -> assertTrue(
                        ((Number) value).longValue()
                                < (long) chunkBytes * chunkCount
                ))
                .jsonPath("$.sha256").isEqualTo(
                        HexFormat.of().formatHex(expectedDigest.digest())
                );

        byte[] binary = webTestClient.get()
                .uri("/test/transport/binary")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith("audio/mpeg")
                .expectHeader().valueEquals(
                        "Content-Disposition",
                        "attachment; filename=\"gateway-test-audio.bin\""
                )
                .expectHeader().valueEquals("Content-Encoding", "identity")
                .expectBody(byte[].class)
                .returnResult()
                .getResponseBody();
        assertNotNull(binary);
        assertEquals(88, binary.length);
        assertEquals((byte) 0xff, binary[0]);
        assertEquals((byte) 0x80, binary[3]);

        List<GatewayInterfaceDefinitionReport.Operation> operations =
                new WebFluxGatewayDefinitionContributor(
                        mappings,
                        reportingProperties,
                        objectMapper
                ).discover().stream()
                        .flatMap(group -> group.interfaceGroup()
                                .operations().stream())
                        .toList();
        assertTrue(operations.stream().noneMatch(operation ->
                "/test/transport/realtime".equals(
                        operation.attributes().get("path")
                )));
    }

    @Test
    void echoesWebSocketTextBinaryPingAndCloseFrames() throws Exception {
        RecordingWebSocketListener listener =
                new RecordingWebSocketListener();
        WebSocket socket = HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .buildAsync(
                        URI.create("ws://127.0.0.1:" + port
                                + "/test/transport/realtime"),
                        listener
                )
                .get(3, TimeUnit.SECONDS);

        socket.sendText("realtime-text", true).get(3, TimeUnit.SECONDS);
        assertEquals(
                "realtime-text",
                listener.text.get(3, TimeUnit.SECONDS)
        );
        byte[] frame = new byte[] {0x00, (byte) 0xff, 0x31};
        socket.sendBinary(ByteBuffer.wrap(frame), true)
                .get(3, TimeUnit.SECONDS);
        assertTrue(Arrays.equals(
                frame,
                listener.binary.get(3, TimeUnit.SECONDS)
        ));
        socket.sendPing(ByteBuffer.wrap(new byte[] {0x01, 0x02}))
                .get(3, TimeUnit.SECONDS);
        assertTrue(Arrays.equals(
                new byte[] {0x01, 0x02},
                listener.pong.get(3, TimeUnit.SECONDS)
        ));
        socket.sendClose(WebSocket.NORMAL_CLOSURE, "contract-complete")
                .get(3, TimeUnit.SECONDS);
        assertEquals(
                WebSocket.NORMAL_CLOSURE,
                listener.closeCode.get(3, TimeUnit.SECONDS)
        );

        RealtimeWebSocketProbe.Snapshot snapshot = webSocketProbe.snapshot();
        assertEquals(1, snapshot.sessions());
        assertEquals(1, snapshot.textFrames());
        assertEquals(1, snapshot.binaryFrames());
    }

    private byte[] uploadChunk(int index, int size) {
        byte[] chunk = new byte[size];
        for (int offset = 0; offset < size; offset++) {
            chunk[offset] = (byte) (index * 31 + offset);
        }
        return chunk;
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties({
            GatewayReportingProperties.class,
            DdcProperties.class
    })
    static class ProviderTestConfiguration {

        @Bean
        RecordingRegistry recordingRegistry() {
            return new RecordingRegistry();
        }

        @Bean
        DdcServiceKeyFactory ddcServiceKeyFactory(DdcProperties properties) {
            return new DdcServiceKeyFactory(properties);
        }

        @Bean
        GatewayDefinitionIdentity gatewayProviderDefinitionIdentity(
                GatewayReportingProperties properties) {
            return new GatewayDefinitionIdentity(
                    "test-webflux-definition-set",
                    properties.getArtifactVersion(),
                    "test-webflux-build"
            );
        }
    }

    static final class RecordingRegistry
            implements DdcServiceRegistryClient {

        private final AtomicInteger registrations = new AtomicInteger();

        private volatile DdcServiceRegistration registration;

        @Override
        public DdcLeaseSession register(DdcServiceRegistration registration) {
            this.registration = registration;
            int sequence = registrations.incrementAndGet();
            Instant now = Instant.now();
            return new DdcLeaseSession(
                    registration.instanceId(),
                    "lease-" + sequence,
                    DdcLeaseRole.HTTP_PROVIDER,
                    registration.leaseSeconds(),
                    registration.heartbeatIntervalSeconds(),
                    now,
                    now.plusSeconds(registration.leaseSeconds())
            );
        }

        @Override
        public DdcLeaseOperationResult heartbeat(
                String instanceId,
                String leaseId) {
            return new DdcLeaseOperationResult(
                    DdcLeaseOperationStatus.RENEWED,
                    Instant.now().plusSeconds(30)
            );
        }

        @Override
        public DdcLeaseOperationResult deregister(
                String instanceId,
                String leaseId) {
            return new DdcLeaseOperationResult(
                    DdcLeaseOperationStatus.DELETED,
                    Instant.now()
            );
        }

        @Override
        public DdcServiceSnapshot getInstances(DdcServiceKey serviceKey) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcRegistrySubscription subscribe(
                DdcServiceKey serviceKey,
                Consumer<DdcServiceSnapshot> listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcServiceCatalogSnapshot getServiceKeys(
                DdcServiceQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcRegistrySubscription subscribeServices(
                DdcServiceQuery query,
                Consumer<DdcServiceCatalogSnapshot> listener) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class RecordingWebSocketListener
            implements WebSocket.Listener {

        private final CompletableFuture<String> text =
                new CompletableFuture<>();

        private final CompletableFuture<byte[]> binary =
                new CompletableFuture<>();

        private final CompletableFuture<byte[]> pong =
                new CompletableFuture<>();

        private final CompletableFuture<Integer> closeCode =
                new CompletableFuture<>();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(
                WebSocket webSocket,
                CharSequence data,
                boolean last) {
            if (last) {
                text.complete(data.toString());
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(
                WebSocket webSocket,
                ByteBuffer data,
                boolean last) {
            byte[] copy = new byte[data.remaining()];
            data.get(copy);
            if (last) {
                binary.complete(copy);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onPong(
                WebSocket webSocket,
                ByteBuffer message) {
            byte[] copy = new byte[message.remaining()];
            message.get(copy);
            pong.complete(copy);
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(
                WebSocket webSocket,
                int statusCode,
                String reason) {
            closeCode.complete(statusCode);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            text.completeExceptionally(error);
            binary.completeExceptionally(error);
            pong.completeExceptionally(error);
            closeCode.completeExceptionally(error);
        }
    }
}
