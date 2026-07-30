package top.egon.cola.component.ddc.management.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import top.egon.cola.component.ddc.security.DdcCanonicalRequest;
import top.egon.cola.component.ddc.security.DdcRequestSigner;

import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class DdcManagementRequestFactory {

    private final DdcManagementClientProperties properties;

    private final ObjectMapper objectMapper;

    private final Clock clock;

    private final Supplier<String> nonceSupplier;

    private final DdcRequestSigner signer = new DdcRequestSigner();

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

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    private static <T> T require(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    public record SignedRequest(
            URI target,
            HttpHeaders headers,
            byte[] body,
            boolean hasBody
    ) {

        public SignedRequest {
            target = require(target, "target");
            headers = HttpHeaders.readOnlyHttpHeaders(
                    new HttpHeaders(require(headers, "headers"))
            );
            body = body == null ? new byte[0] : body.clone();
        }

        @Override
        public byte[] body() {
            return body.clone();
        }
    }
}
