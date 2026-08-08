package top.egon.cola.component.ddc.management.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import top.egon.cola.component.ddc.model.security.DdcCanonicalRequest;
import top.egon.cola.component.ddc.model.security.DdcRequestSigner;

import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 将管理请求序列化为规范 HTTP 请求，并生成完整 HMAC 认证头。 /
 * Serializes management requests into canonical HTTP requests with complete HMAC authentication headers.
 */
public final class DdcManagementRequestFactory {

    /**
     * 客户端地址与 HMAC 凭据配置。 / Client endpoint and HMAC credential settings.
     */
    private final DdcManagementClientProperties properties;

    /**
     * 请求体 JSON 序列化器。 / JSON serializer for request bodies.
     */
    private final ObjectMapper objectMapper;

    /**
     * 生成签名时间戳的时钟。 / Clock used to generate signature timestamps.
     */
    private final Clock clock;

    /**
     * 生成防重放随机数的供应器。 / Supplier used to generate anti-replay nonces.
     */
    private final Supplier<String> nonceSupplier;

    /**
     * HMAC 规范请求签名器。 / HMAC canonical-request signer.
     */
    private final DdcRequestSigner signer = new DdcRequestSigner();

    /**
     * 使用 UTC 系统时钟与随机 UUID 防重放值构造请求工厂。 /
     * Constructs a request factory using the UTC system clock and random UUID nonces.
     *
     * @param properties   客户端地址与凭据配置 / client endpoint and credential settings
     * @param objectMapper 请求体 JSON 序列化器 / request-body JSON serializer
     * @throws IllegalArgumentException 当任一依赖为空时 / when any dependency is null
     */
    public DdcManagementRequestFactory(
            DdcManagementClientProperties properties,
            ObjectMapper objectMapper
    ) {
        this(
                properties,
                objectMapper,
                Clock.systemUTC(),
                () -> UUID.randomUUID().toString()
        );
    }

    /**
     * 使用可控时钟与防重放值供应器构造请求工厂，供包内测试与定制使用。 /
     * Constructs a request factory with controllable clock and nonce supplier for package-level testing and customization.
     *
     * @param properties    客户端地址与凭据配置 / client endpoint and credential settings
     * @param objectMapper  请求体 JSON 序列化器 / request-body JSON serializer
     * @param clock         生成签名时间戳的时钟 / clock used for signature timestamps
     * @param nonceSupplier 生成防重放值的供应器 / supplier used for anti-replay nonces
     * @throws IllegalArgumentException 当任一依赖为空时 / when any dependency is null
     */
    DdcManagementRequestFactory(
            DdcManagementClientProperties properties,
            ObjectMapper objectMapper,
            Clock clock,
            Supplier<String> nonceSupplier
    ) {
        this.properties = require(properties, "properties");
        this.objectMapper = require(objectMapper, "objectMapper");
        this.clock = require(clock, "clock");
        this.nonceSupplier = require(nonceSupplier, "nonceSupplier");
    }

    /**
     * 创建已编码查询参数、可选 JSON 请求体与 HMAC 认证头的签名请求。 /
     * Creates a signed request with encoded query parameters, optional JSON body, and HMAC authentication headers.
     *
     * @param method  HTTP 方法 / HTTP method
     * @param path    未编码的管理接口路径 / unencoded management API path
     * @param query   查询参数，多值会参与规范排序与签名 / query parameters whose values participate in canonical sorting and signing
     * @param request 请求对象；空值表示无请求体 / request object; null means no request body
     * @return 可直接交给 REST 客户端发送的签名请求 / signed request ready for REST-client dispatch
     * @throws IllegalArgumentException     当方法、路径或防重放值无效时 / when the method, path, or nonce is invalid
     * @throws DdcManagementClientException 当请求体无法序列化时 / when the request body cannot be serialized
     */
    public SignedRequest create(
            HttpMethod method,
            String path,
            Map<String, List<String>> query,
            Object request
    ) {
        require(method, "method");
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
        headers.set(DdcRequestSigner.ACCESS_KEY_HEADER, properties.accessKey());
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
                signer.sign(canonicalRequest, properties.secretKey())
        );
        return new SignedRequest(URI.create(target), headers, body, request != null);
    }

    /**
     * 将请求对象序列化为 JSON 字节。 / Serializes a request object to JSON bytes.
     *
     * @param request 待序列化请求对象 / request object to serialize
     * @return JSON 请求体字节 / JSON request-body bytes
     * @throws DdcManagementClientException 当 JSON 序列化失败时 / when JSON serialization fails
     */
    private byte[] serialize(Object request) {
        try {
            return objectMapper.writeValueAsBytes(request);
        } catch (JsonProcessingException exception) {
            throw new DdcManagementClientException(
                    "DDC_MANAGEMENT_SERIALIZATION_ERROR",
                    "DDC management request serialization failed",
                    exception
            );
        }
    }

    /**
     * 要求文本非空。 / Requires nonblank text.
     *
     * @param value     待校验文本 / text to validate
     * @param fieldName 用于错误消息的字段名 / field name used in error messages
     * @return 原始非空文本 / original nonblank text
     * @throws IllegalArgumentException 当文本为空时 / when the text is blank
     */
    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    /**
     * 要求引用非空。 / Requires a nonnull reference.
     *
     * @param <T>       引用类型 / reference type
     * @param value     待校验引用 / reference to validate
     * @param fieldName 用于错误消息的字段名 / field name used in error messages
     * @return 原始非空引用 / original nonnull reference
     * @throws IllegalArgumentException 当引用为空时 / when the reference is null
     */
    private static <T> T require(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    /**
     * 包含目标 URI、只读认证头、防御性复制请求体及请求体存在标记的签名请求。 /
     * Signed request containing target URI, read-only authentication headers, defensively copied body, and body-presence flag.
     *
     * @param target  含规范查询字符串的相对目标 URI / relative target URI including the canonical query string
     * @param headers 只读 HTTP 认证头 / read-only HTTP authentication headers
     * @param body    请求体字节 / request-body bytes
     * @param hasBody 是否应发送请求体，即使序列化结果为空 / whether a request body should be sent even if its serialized bytes are empty
     */
    public record SignedRequest(
            URI target,
            HttpHeaders headers,
            byte[] body,
            boolean hasBody
    ) {

        /**
         * 校验必填值，并冻结认证头及防御性复制请求体。 /
         * Validates required values, freezes authentication headers, and defensively copies the body.
         *
         * @throws IllegalArgumentException 当目标 URI 或认证头为空时 / when the target URI or headers are null
         */
        public SignedRequest {
            target = require(target, "target");
            headers = HttpHeaders.readOnlyHttpHeaders(
                    new HttpHeaders(require(headers, "headers"))
            );
            body = body == null ? new byte[0] : body.clone();
        }

        /**
         * 返回请求体的防御性副本。 / Returns a defensive copy of the request body.
         *
         * @return 新复制的请求体字节 / newly copied request-body bytes
         */
        @Override
        public byte[] body() {
            return body.clone();
        }
    }
}
