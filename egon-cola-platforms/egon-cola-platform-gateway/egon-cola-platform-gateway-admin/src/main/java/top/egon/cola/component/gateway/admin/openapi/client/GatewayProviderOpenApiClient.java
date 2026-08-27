package top.egon.cola.component.gateway.admin.openapi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.gateway.admin.openapi.domain.dto.GatewayOpenApiDocumentDTO;
import top.egon.cola.component.gateway.admin.openapi.domain.dto.GatewayOpenApiSyncCandidateDTO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Fetches one trusted Provider OpenAPI Group using JDK HttpClient.
 *
 * <p>URL derivation, OAuth audience, double DNS validation, no-redirect
 * behavior and bounded streaming are kept in this technical boundary. It
 * never writes snapshots or Definitions.</p>
 */
@Slf4j
@Validated
@Component("gatewayProviderOpenApiClient")
@ConditionalOnProperty(
        name = "gateway.admin.openapi.enabled",
        havingValue = "true"
)
@RequiredArgsConstructor
public class GatewayProviderOpenApiClient {

    public static final String OPENAPI_READ_SCOPE = "gateway.openapi.read";

    @Qualifier("gatewayOpenApiTokenSupplier")
    private final GatewayOpenApiTokenSupplier tokenSupplier;

    @Qualifier("gatewayOpenApiDnsPolicy")
    private final GatewayOpenApiDnsPolicy dnsPolicy;

    @Qualifier("gatewayOpenApiHttpClient")
    private final HttpClient httpClient;

    @Qualifier("gatewayOpenApiObjectMapper")
    private final ObjectMapper objectMapper;

    @Qualifier("gatewayOpenApiClock")
    private final Clock clock;

    @Qualifier("gatewayOpenApiReadTimeout")
    private final Duration readTimeout;

    @Qualifier("gatewayOpenApiMaximumDocumentBytes")
    private final int maximumDocumentBytes;

    /**
     * Fetches and parses one manifest-selected Group document.
     *
     * @param candidate trusted DDC-derived target
     * @return bounded raw document with exact SHA and parsed JSON
     */
    public GatewayOpenApiDocumentDTO fetch(
            @Valid GatewayOpenApiSyncCandidateDTO candidate) {
        Objects.requireNonNull(candidate, "candidate");
        try {
            dnsPolicy.resolveAndValidate(candidate.host());
            URI target = targetUri(candidate);
            String token = requiredToken(tokenSupplier.tokenFor(
                    candidate.resourceUri(),
                    OPENAPI_READ_SCOPE
            ));
            dnsPolicy.resolveAndValidate(candidate.host());
            HttpRequest request = HttpRequest.newBuilder(target)
                    .timeout(readTimeout)
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream()
            );
            int status = response.statusCode();
            String contentType;
            byte[] raw;
            try (InputStream body = response.body()) {
                if (status == 401 || status == 403) {
                    throw new GatewayOpenApiFetchException(
                            "GATEWAY_OPENAPI_FETCH_FORBIDDEN",
                            false,
                            "provider denied OpenAPI read"
                    );
                }
                if (status < 200 || status >= 300) {
                    throw new GatewayOpenApiFetchException(
                            "GATEWAY_OPENAPI_HTTP_STATUS",
                            status >= 500,
                            "provider returned an unacceptable HTTP status"
                    );
                }
                contentType = response.headers()
                        .firstValue("Content-Type")
                        .orElseThrow(() -> new GatewayOpenApiFetchException(
                                "GATEWAY_OPENAPI_CONTENT_TYPE",
                                false,
                                "provider response is not JSON"
                        ));
                if (!jsonContentType(contentType)) {
                    throw new GatewayOpenApiFetchException(
                            "GATEWAY_OPENAPI_CONTENT_TYPE",
                            false,
                            "provider response is not JSON"
                    );
                }
                raw = readBounded(body);
            }
            JsonNode document = parse(raw);
            return new GatewayOpenApiDocumentDTO(
                    candidate,
                    raw,
                    sha256(raw),
                    document,
                    contentType,
                    status,
                    clock.instant()
            );
        } catch (GatewayOpenApiFetchException failure) {
            log.warn(
                    "Provider OpenAPI fetch failed code={} app={} build={} "
                            + "group={} instance={}",
                    failure.errorCode(),
                    candidate.applicationId(),
                    candidate.buildId(),
                    candidate.openapiGroup(),
                    candidate.instanceId()
            );
            throw failure;
        } catch (java.net.http.HttpTimeoutException failure) {
            throw classified(
                    candidate,
                    "GATEWAY_OPENAPI_FETCH_TIMEOUT",
                    true,
                    "provider OpenAPI fetch timed out"
            );
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw classified(
                    candidate,
                    "GATEWAY_OPENAPI_FETCH_INTERRUPTED",
                    true,
                    "provider OpenAPI fetch was interrupted"
            );
        } catch (IOException failure) {
            throw classified(
                    candidate,
                    "GATEWAY_OPENAPI_FETCH_IO",
                    true,
                    "provider OpenAPI fetch failed"
            );
        } catch (RuntimeException failure) {
            throw classified(
                    candidate,
                    "GATEWAY_OPENAPI_FETCH_FAILED",
                    true,
                    "provider OpenAPI fetch failed"
            );
        }
    }

    private URI targetUri(GatewayOpenApiSyncCandidateDTO candidate) {
        String host = candidate.host().indexOf(':') >= 0
                ? "[" + candidate.host() + "]"
                : candidate.host();
        String path = candidate.pathTemplate().replace(
                "{group}",
                candidate.openapiGroup()
        );
        String scheme = candidate.secure()
                ? "https"
                : candidate.developmentPlaintext()
                ? "http"
                : null;
        if (scheme == null) {
            throw new GatewayOpenApiFetchException(
                    "GATEWAY_OPENAPI_TARGET_FORBIDDEN",
                    false,
                    "provider target must use HTTPS"
            );
        }
        try {
            URI target = URI.create(
                    scheme + "://" + host + ":" + candidate.port() + path
            );
            if (!("https".equalsIgnoreCase(target.getScheme())
                    || (candidate.developmentPlaintext()
                    && "http".equalsIgnoreCase(target.getScheme())))
                    || target.getRawQuery() != null
                    || target.getRawFragment() != null
                    || target.getUserInfo() != null) {
                throw new IllegalArgumentException("unsafe target URI");
            }
            return target;
        } catch (IllegalArgumentException failure) {
            throw new GatewayOpenApiFetchException(
                    "GATEWAY_OPENAPI_TARGET_INVALID",
                    false,
                    "provider target URI is invalid"
            );
        }
    }

    private byte[] readBounded(InputStream body) throws IOException {
        if (maximumDocumentBytes <= 0
                || maximumDocumentBytes
                > GatewayOpenApiDocumentDTO.MAX_DOCUMENT_BYTES) {
            throw new GatewayOpenApiFetchException(
                    "GATEWAY_OPENAPI_LIMIT_CONFIGURATION",
                    false,
                    "OpenAPI document limit configuration is invalid"
            );
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream(
                Math.min(maximumDocumentBytes, 8192)
        );
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = body.read(buffer)) >= 0) {
            if (read == 0) {
                continue;
            }
            total += read;
            if (total > maximumDocumentBytes) {
                throw new GatewayOpenApiFetchException(
                        "GATEWAY_OPENAPI_DOCUMENT_TOO_LARGE",
                        false,
                        "provider OpenAPI document exceeds the configured limit"
                );
            }
            output.write(buffer, 0, read);
        }
        if (total == 0) {
            throw new GatewayOpenApiFetchException(
                    "GATEWAY_OPENAPI_DOCUMENT_EMPTY",
                    false,
                    "provider OpenAPI document is empty"
            );
        }
        return output.toByteArray();
    }

    private JsonNode parse(byte[] raw) {
        try {
            JsonNode document = objectMapper.readTree(raw);
            if (document == null || !document.isObject()) {
                throw new GatewayOpenApiFetchException(
                        "GATEWAY_OPENAPI_JSON_OBJECT",
                        false,
                        "provider OpenAPI response must be a JSON object"
                );
            }
            return document;
        } catch (IOException failure) {
            throw new GatewayOpenApiFetchException(
                    "GATEWAY_OPENAPI_INVALID_JSON",
                    false,
                    "provider response is not valid JSON"
            );
        }
    }

    private static boolean jsonContentType(String value) {
        String mediaType = value.split(";", 2)[0].trim();
        return "application/json".equalsIgnoreCase(mediaType);
    }

    private static String requiredToken(String value) {
        if (value == null || value.isBlank() || value.length() > 8192
                || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            throw new GatewayOpenApiFetchException(
                    "GATEWAY_OPENAPI_OAUTH_FAILED",
                    false,
                    "OpenAPI service token was unavailable"
            );
        }
        return value;
    }

    private GatewayOpenApiFetchException classified(
            GatewayOpenApiSyncCandidateDTO candidate,
            String code,
            boolean retryable,
            String message) {
        log.warn(
                "Provider OpenAPI fetch failed code={} app={} build={} "
                        + "group={} instance={}",
                code,
                candidate.applicationId(),
                candidate.buildId(),
                candidate.openapiGroup(),
                candidate.instanceId()
        );
        return new GatewayOpenApiFetchException(code, retryable, message);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes)
            );
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    failure
            );
        }
    }
}
