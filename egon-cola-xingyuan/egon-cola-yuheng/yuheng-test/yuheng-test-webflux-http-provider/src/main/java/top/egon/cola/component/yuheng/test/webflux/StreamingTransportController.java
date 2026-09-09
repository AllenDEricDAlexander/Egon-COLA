package top.egon.cola.component.yuheng.test.webflux;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/test/transport")
@Tag(name = "inventory-reactive")
@EgonApiCatalog(
        businessDomainCode = "yuheng-test",
        businessDomainName = "网关测试域",
        entityDomainCode = "streaming-transport",
        entityDomainName = "流式传输实体域",
        interfaceGroupCode = "inventory-reactive"
)
public class StreamingTransportController {

    private static final byte[] BINARY_CHUNK = new byte[] {
            (byte) 0xff, (byte) 0xd8, 0x00, (byte) 0x80,
            0x41, 0x55, 0x44, 0x49, 0x4f, 0x00, (byte) 0xfe
    };

    @PostMapping(
            path = "/json",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            operationId = "inventory-reactive.json",
            summary = "不解析请求内容并按 DataBuffer 回显 JSON",
            tags = {"streaming", "json", "openai"}
    )
    @EgonGatewayPolicy(
            owner = "yuheng-test",
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.FALSE
    )
    public Flux<DataBuffer> json(@Parameter(hidden = true)
                                 ServerHttpRequest request) {
        return request.getBody();
    }

    @GetMapping(
            path = "/sse",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    @Operation(
            operationId = "inventory-reactive.sse",
            summary = "逐事件发送 OpenAI 风格 SSE 响应",
            tags = {"streaming", "sse", "openai"}
    )
    @EgonGatewayPolicy(
            owner = "yuheng-test",
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.FALSE
    )
    public Flux<DataBuffer> sse(@Parameter(hidden = true)
                                ServerHttpResponse response) {
        response.getHeaders().setCacheControl("no-cache");
        response.getHeaders().add("X-Accel-Buffering", "no");
        return Flux.just(
                        "data: {\"type\":\"response.output_text.delta\","
                                + "\"delta\":\"hello\"}\n\n",
                        "data: [DONE]\n\n"
                )
                .delayElements(Duration.ofMillis(25))
                .map(value -> response.bufferFactory().wrap(
                        value.getBytes(java.nio.charset.StandardCharsets.UTF_8)
                ));
    }

    @PostMapping(
            path = "/upload",
            consumes = {
                    MediaType.MULTIPART_FORM_DATA_VALUE,
                    MediaType.APPLICATION_OCTET_STREAM_VALUE
            },
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            operationId = "inventory-reactive.upload",
            summary = "直接消费 DataBuffer 并增量计算上传摘要",
            tags = {"streaming", "multipart", "upload"}
    )
    @EgonGatewayPolicy(
            owner = "yuheng-test",
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.FALSE
    )
    public Mono<UploadSummary> upload(@Parameter(hidden = true)
                                     ServerHttpRequest request) {
        MessageDigest digest = sha256();
        AtomicLong bytes = new AtomicLong();
        AtomicInteger chunks = new AtomicInteger();
        AtomicInteger maxChunkBytes = new AtomicInteger();

        return request.getBody()
                .doOnNext(buffer -> update(
                        buffer,
                        digest,
                        bytes,
                        chunks,
                        maxChunkBytes
                ))
                .then(Mono.fromSupplier(() -> new UploadSummary(
                        bytes.get(),
                        chunks.get(),
                        maxChunkBytes.get(),
                        HexFormat.of().formatHex(digest.digest())
                )));
    }

    @GetMapping(
            path = "/binary",
            produces = "audio/mpeg"
    )
    @Operation(
            operationId = "inventory-reactive.binary",
            summary = "按 DataBuffer 发送包含非 UTF-8 字节的音频响应",
            tags = {"streaming", "binary", "audio"}
    )
    @EgonGatewayPolicy(
            owner = "yuheng-test",
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.FALSE
    )
    public Flux<DataBuffer> binary(@Parameter(hidden = true)
                                   ServerHttpResponse response) {
        response.getHeaders().setContentDisposition(
                ContentDisposition.attachment()
                        .filename("yuheng-test-audio.bin")
                        .build()
        );
        response.getHeaders().set(
                HttpHeaders.CONTENT_ENCODING,
                "identity"
        );
        return Flux.range(0, 8)
                .map(ignored -> response.bufferFactory().wrap(
                        BINARY_CHUNK.clone()
                ));
    }

    private void update(
            DataBuffer buffer,
            MessageDigest digest,
            AtomicLong bytes,
            AtomicInteger chunks,
            AtomicInteger maxChunkBytes) {
        int readableBytes = buffer.readableByteCount();
        try (DataBuffer.ByteBufferIterator iterator =
                     buffer.readableByteBuffers()) {
            while (iterator.hasNext()) {
                ByteBuffer byteBuffer = iterator.next().asReadOnlyBuffer();
                digest.update(byteBuffer);
            }
            bytes.addAndGet(readableBytes);
            chunks.incrementAndGet();
            maxChunkBytes.accumulateAndGet(readableBytes, Math::max);
        }
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record UploadSummary(
            long bytes,
            int chunks,
            int maxChunkBytes,
            String sha256) {
    }
}
