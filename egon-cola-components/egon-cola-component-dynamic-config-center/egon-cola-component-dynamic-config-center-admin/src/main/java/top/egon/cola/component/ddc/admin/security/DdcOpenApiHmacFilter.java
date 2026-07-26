package top.egon.cola.component.ddc.admin.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.component.common.result.factory.ResultDtos;
import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;
import top.egon.cola.component.ddc.common.DdcErrorStatus;
import top.egon.cola.component.ddc.security.DdcCanonicalRequest;
import top.egon.cola.component.ddc.security.DdcRequestSigner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class DdcOpenApiHmacFilter extends OncePerRequestFilter {

    private static final String OPEN_API_PREFIX = "/api/v1/ddc/openapi/";

    private final DdcAdminProperties properties;

    private final ObjectMapper objectMapper;

    private final DdcNonceStore nonceStore;

    private final Clock clock;

    private final DdcRequestSigner signer = new DdcRequestSigner();

    @Autowired
    public DdcOpenApiHmacFilter(ObjectMapper objectMapper,
                                ObjectProvider<DdcAdminProperties> propertiesProvider,
                                ObjectProvider<DdcNonceStore> nonceStoreProvider) {
        this(
                propertiesProvider.getIfAvailable(DdcAdminProperties::new),
                objectMapper,
                nonceStoreProvider.getIfAvailable(),
                Clock.systemUTC()
        );
    }

    DdcOpenApiHmacFilter(DdcAdminProperties properties,
                         ObjectMapper objectMapper,
                         DdcNonceStore nonceStore,
                         Clock clock) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.nonceStore = nonceStore;
        this.clock = clock;
        validateConfiguration(properties.getOpenapi());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(OPEN_API_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        DdcAdminProperties.Openapi openapi = properties.getOpenapi();
        if (!openapi.isSignatureEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        DdcCachedBodyHttpServletRequest cachedRequest = new DdcCachedBodyHttpServletRequest(request);
        Headers headers = headers(cachedRequest);
        if (headers == null) {
            reject(response, DdcErrorStatus.SIGNATURE_REQUIRED);
            return;
        }

        Long timestamp = parseTimestamp(headers.timestamp());
        if (timestamp == null) {
            reject(response, DdcErrorStatus.SIGNATURE_INVALID);
            return;
        }
        long now = clock.millis();
        long allowedSkewMillis = openapi.getAllowedClockSkewSeconds() * 1000L;
        if (timestamp < now - allowedSkewMillis || timestamp > now + allowedSkewMillis) {
            reject(response, DdcErrorStatus.SIGNATURE_EXPIRED);
            return;
        }
        String accessKey = accessKey(openapi);
        String secret = secret(openapi);
        if (!signer.matches(accessKey, headers.accessKey())) {
            reject(response, DdcErrorStatus.SIGNATURE_INVALID);
            return;
        }

        DdcCanonicalRequest canonicalRequest = new DdcCanonicalRequest(
                request.getMethod(),
                request.getRequestURI(),
                query(request),
                timestamp,
                headers.nonce(),
                cachedRequest.body()
        );
        if (!signer.matches(canonicalRequest.contentSha256(), headers.contentSha256())
                || !signer.matches(signer.sign(canonicalRequest, secret), headers.signature())) {
            reject(response, DdcErrorStatus.SIGNATURE_INVALID);
            return;
        }

        try {
            if (nonceStore == null) {
                throw new IllegalStateException(
                        "DDC nonce store is unavailable"
                );
            }
            Duration nonceTtl = Duration.ofMillis(Math.max(
                    1,
                    timestamp + allowedSkewMillis - now
            ));
            if (!nonceStore.markIfAbsent(
                    credentialId(openapi),
                    headers.nonce(),
                    nonceTtl
            )) {
                reject(response, DdcErrorStatus.SIGNATURE_REPLAY);
                return;
            }
        } catch (RuntimeException unavailable) {
            if (writeRequest(request.getMethod())) {
                rejectNonceStoreUnavailable(response);
                return;
            }
        }
        filterChain.doFilter(cachedRequest, response);
    }

    private Headers headers(HttpServletRequest request) {
        Headers headers = new Headers(
                request.getHeader(DdcRequestSigner.ACCESS_KEY_HEADER),
                request.getHeader(DdcRequestSigner.TIMESTAMP_HEADER),
                request.getHeader(DdcRequestSigner.NONCE_HEADER),
                request.getHeader(DdcRequestSigner.CONTENT_SHA256_HEADER),
                request.getHeader(DdcRequestSigner.SIGNATURE_HEADER)
        );
        return headers.complete() ? headers : null;
    }

    private void validateConfiguration(DdcAdminProperties.Openapi openapi) {
        if (openapi.getAllowedClockSkewSeconds() <= 0) {
            throw new IllegalStateException("DDC OpenAPI allowed clock skew must be positive");
        }
        if (openapi.isSignatureEnabled()
                && (!hasText(accessKey(openapi))
                || !hasText(secret(openapi)))) {
            throw new IllegalStateException(
                    "DDC OpenAPI access key and secret key are required when signatures are enabled"
            );
        }
    }

    private String accessKey(DdcAdminProperties.Openapi openapi) {
        if (hasText(openapi.getAccessKey())) {
            return openapi.getAccessKey();
        }
        return firstCredential(openapi)
                .map(DdcAdminProperties.Credential::getAccessKey)
                .orElse(null);
    }

    private String secret(DdcAdminProperties.Openapi openapi) {
        if (hasText(openapi.getSecretKey())) {
            return openapi.getSecretKey();
        }
        return firstCredential(openapi)
                .map(DdcAdminProperties.Credential::getSecret)
                .orElse(null);
    }

    private String credentialId(DdcAdminProperties.Openapi openapi) {
        return firstCredential(openapi)
                .map(DdcAdminProperties.Credential::getCredentialId)
                .filter(this::hasText)
                .orElseGet(() -> accessKey(openapi));
    }

    private Optional<DdcAdminProperties.Credential> firstCredential(
            DdcAdminProperties.Openapi openapi) {
        return openapi.getCredentials() == null
                || openapi.getCredentials().isEmpty()
                ? Optional.empty()
                : Optional.of(openapi.getCredentials().getFirst());
    }

    private Map<String, List<String>> query(HttpServletRequest request) {
        Map<String, List<String>> query = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) ->
                query.put(key, values == null ? List.of() : Arrays.asList(values)));
        return query;
    }

    private Long parseTimestamp(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void reject(HttpServletResponse response, DdcErrorStatus status) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ResultDtos.failure(status));
    }

    private void rejectNonceStoreUnavailable(HttpServletResponse response)
            throws IOException {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "code", "DDC_NONCE_STORE_UNAVAILABLE",
                "message", "DDC nonce store is unavailable"
        ));
    }

    private boolean writeRequest(String method) {
        return !"GET".equalsIgnoreCase(method)
                && !"HEAD".equalsIgnoreCase(method)
                && !"OPTIONS".equalsIgnoreCase(method);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record Headers(
            String accessKey,
            String timestamp,
            String nonce,
            String contentSha256,
            String signature
    ) {

        private boolean complete() {
            return hasText(accessKey)
                    && hasText(timestamp)
                    && hasText(nonce)
                    && hasText(contentSha256)
                    && hasText(signature);
        }

        private boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }
}
