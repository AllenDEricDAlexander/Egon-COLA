package top.egon.cola.component.ddc.client.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import top.egon.cola.component.ddc.error.http.DdcOpenApiRequestException;
import top.egon.cola.component.ddc.observability.DdcTraceSupport;

import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 统一构造 DDC OpenAPI 的规范目标、JSON 请求体、追踪头和可选 HMAC 认证头。 /
 * Builds canonical targets, JSON bodies, trace headers, and optional HMAC authentication headers for DDC OpenAPI calls.
 */
public final class DdcOpenApiRequestFactory {

    private final ObjectMapper objectMapper;

    private final Clock clock;

    private final Supplier<String> nonceSupplier;

    private final boolean signatureEnabled;

    private final String accessKey;

    private final String secretKey;

    private final DdcRequestSigner signer = new DdcRequestSigner();

    /**
     * 使用系统时钟和随机 UUID 创建请求工厂。 /
     * Creates a request factory using the system clock and random UUID nonces.
     *
     * @param objectMapper      JSON 映射器 / JSON mapper
     * @param signatureEnabled 是否生成 HMAC 认证头 / whether HMAC headers are generated
     * @param accessKey        HMAC Access Key / HMAC access key
     * @param secretKey        HMAC Secret Key / HMAC secret key
     */
    public DdcOpenApiRequestFactory(ObjectMapper objectMapper,
                                    boolean signatureEnabled,
                                    @Nullable String accessKey,
                                    @Nullable String secretKey) {
        this(
                objectMapper,
                Clock.systemUTC(),
                () -> UUID.randomUUID().toString(),
                signatureEnabled,
                accessKey,
                secretKey
        );
    }

    /**
     * 使用可控时钟和 nonce 供应器创建请求工厂。 /
     * Creates a request factory with a controllable clock and nonce supplier.
     *
     * @param objectMapper      JSON 映射器 / JSON mapper
     * @param clock             签名时间戳时钟 / signature timestamp clock
     * @param nonceSupplier     防重放 nonce 供应器 / anti-replay nonce supplier
     * @param signatureEnabled 是否生成 HMAC 认证头 / whether HMAC headers are generated
     * @param accessKey        HMAC Access Key / HMAC access key
     * @param secretKey        HMAC Secret Key / HMAC secret key
     */
    public DdcOpenApiRequestFactory(ObjectMapper objectMapper,
                                    Clock clock,
                                    Supplier<String> nonceSupplier,
                                    boolean signatureEnabled,
                                    @Nullable String accessKey,
                                    @Nullable String secretKey) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.nonceSupplier = Objects.requireNonNull(nonceSupplier, "nonceSupplier");
        this.signatureEnabled = signatureEnabled;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        if (signatureEnabled) {
            requireText(accessKey, "accessKey");
            requireText(secretKey, "secretKey");
        }
    }

    /**
     * 创建可直接交给 Spring RestClient 的不可变请求。 /
     * Creates an immutable request ready for Spring RestClient.
     *
     * @param method  HTTP 方法 / HTTP method
     * @param path    相对请求路径 / relative request path
     * @param query   多值查询参数 / multi-valued query parameters
     * @param request 可选请求对象 / optional request object
     * @return 规范化且已准备请求头的请求 / canonical request with prepared headers
     */
    public Request create(HttpMethod method,
                          String path,
                          @Nullable Map<String, List<String>> query,
                          @Nullable Object request) {
        Objects.requireNonNull(method, "method");
        byte[] body = request == null ? new byte[0] : serialize(request);
        DdcCanonicalRequest canonicalRequest = new DdcCanonicalRequest(
                method.name(),
                requireText(path, "path"),
                query == null ? Map.of() : query,
                clock.millis(),
                requireText(nonceSupplier.get(), "nonce"),
                body
        );
        String target = canonicalRequest.canonicalQuery().isEmpty()
                ? path
                : path + "?" + canonicalRequest.canonicalQuery();
        HttpHeaders headers = new HttpHeaders();
        DdcTraceSupport.inject(headers);
        if (signatureEnabled) {
            headers.set(DdcRequestSigner.ACCESS_KEY_HEADER, accessKey);
            headers.set(
                    DdcRequestSigner.TIMESTAMP_HEADER,
                    Long.toString(canonicalRequest.timestamp())
            );
            headers.set(DdcRequestSigner.NONCE_HEADER, canonicalRequest.nonce());
            headers.set(
                    DdcRequestSigner.CONTENT_SHA256_HEADER,
                    canonicalRequest.contentSha256()
            );
            headers.set(
                    DdcRequestSigner.SIGNATURE_HEADER,
                    signer.sign(canonicalRequest, secretKey)
            );
        }
        return new Request(URI.create(target), headers, body, request != null);
    }

    private byte[] serialize(Object request) {
        try {
            return objectMapper.writeValueAsBytes(request);
        } catch (JsonProcessingException exception) {
            throw new DdcOpenApiRequestException(exception);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    /**
     * 包含目标 URI、只读请求头、防御性复制请求体和正文存在标记的请求。 /
     * Request containing a target URI, read-only headers, a defensively copied body, and a body-presence flag.
     *
     * @param target  目标 URI / target URI
     * @param headers 只读请求头 / read-only headers
     * @param body    请求体 / request body
     * @param hasBody 是否发送正文 / whether a body is sent
     */
    public record Request(URI target,
                          HttpHeaders headers,
                          byte[] body,
                          boolean hasBody) {

        public Request {
            target = Objects.requireNonNull(target, "target");
            headers = HttpHeaders.readOnlyHttpHeaders(
                    new HttpHeaders(Objects.requireNonNull(headers, "headers"))
            );
            body = body == null ? new byte[0] : body.clone();
        }

        @Override
        public byte[] body() {
            return body.clone();
        }
    }
}
