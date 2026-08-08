package top.egon.cola.component.ddc.admin.security.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriUtils;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;
import top.egon.cola.component.ddc.common.DdcErrorStatus;
import top.egon.cola.component.ddc.model.security.DdcCanonicalRequest;
import top.egon.cola.component.ddc.model.security.DdcRequestSigner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class DdcOpenApiHmacFilter extends OncePerRequestFilter {

    private static final String OPEN_API_PREFIX = "/api/v1/ddc/openapi/";

    private final DdcAdminProperties properties;

    private final ObjectMapper objectMapper;

    private final DdcNonceStore nonceStore;

    private final DdcHmacCredentialRegistry credentialRegistry;

    private final Clock clock;

    private final DdcRequestSigner signer = new DdcRequestSigner();

    @Autowired
    public DdcOpenApiHmacFilter(ObjectMapper objectMapper,
                                ObjectProvider<DdcAdminProperties> propertiesProvider,
                                ObjectProvider<DdcNonceStore> nonceStoreProvider,
                                ObjectProvider<DdcHmacCredentialRegistry>
                                        credentialRegistryProvider) {
        this(
                propertiesProvider.getIfAvailable(DdcAdminProperties::new),
                objectMapper,
                nonceStoreProvider.getIfAvailable(),
                credentialRegistry(
                        propertiesProvider,
                        credentialRegistryProvider
                ),
                Clock.systemUTC()
        );
    }

    DdcOpenApiHmacFilter(DdcAdminProperties properties,
                         ObjectMapper objectMapper,
                         DdcNonceStore nonceStore,
                         Clock clock) {
        this(
                properties,
                objectMapper,
                nonceStore,
                new DdcHmacCredentialRegistry(properties),
                clock
        );
    }

    DdcOpenApiHmacFilter(
            DdcAdminProperties properties,
            ObjectMapper objectMapper,
            DdcNonceStore nonceStore,
            DdcHmacCredentialRegistry credentialRegistry,
            Clock clock) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.nonceStore = nonceStore;
        this.credentialRegistry = credentialRegistry;
        this.clock = clock;
        validateConfiguration(properties.getOpenapi(), credentialRegistry);
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
        DdcHmacCredential credential = credentialRegistry.resolve(
                headers.accessKey()
        ).orElse(null);
        if (credential == null) {
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
                || !signer.matches(
                        signer.sign(canonicalRequest, credential.secret()),
                        headers.signature()
                )) {
            reject(response, DdcErrorStatus.SIGNATURE_INVALID);
            return;
        }

        RequestScope scope = requestScope(cachedRequest);
        if (scope == null || !credential.permits(
                scope.clientType(),
                scope.operation(),
                scope.appCode(),
                scope.env(),
                scope.bizCode()
        )) {
            rejectScope(response);
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
                    credential.credentialId(),
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
        attachPrincipal(
                cachedRequest,
                credential.principal(
                        scope.appCode(),
                        scope.env(),
                        scope.bizCode()
                )
        );
        try {
            filterChain.doFilter(cachedRequest, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
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

    private void validateConfiguration(
            DdcAdminProperties.Openapi openapi,
            DdcHmacCredentialRegistry registry) {
        if (openapi.getAllowedClockSkewSeconds() <= 0) {
            throw new IllegalStateException("DDC OpenAPI allowed clock skew must be positive");
        }
        if (openapi.isSignatureEnabled() && registry.isEmpty()) {
            throw new IllegalStateException(
                    "DDC OpenAPI access key and secret key are required when signatures are enabled"
            );
        }
    }

    private static DdcHmacCredentialRegistry credentialRegistry(
            ObjectProvider<DdcAdminProperties> propertiesProvider,
            ObjectProvider<DdcHmacCredentialRegistry> registryProvider) {
        return registryProvider.getIfAvailable(() ->
                new DdcHmacCredentialRegistry(
                        propertiesProvider.getIfAvailable(
                                DdcAdminProperties::new
                        )
                )
        );
    }

    private RequestScope requestScope(
            DdcCachedBodyHttpServletRequest request) {
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        String relative = request.getRequestURI().substring(
                OPEN_API_PREFIX.length()
        );
        JsonNode body = body(request.body());
        if ("POST".equals(method)
                && "instances/register".equals(relative)) {
            return bodyScope("SDK", "SDK_REGISTER", body);
        }
        if ("POST".equals(method)
                && "instances/heartbeat".equals(relative)) {
            return bodyScope("SDK", "SDK_HEARTBEAT", body);
        }
        if ("POST".equals(method)
                && "instances/offline".equals(relative)) {
            return bodyScope("SDK", "SDK_OFFLINE", body);
        }
        if ("GET".equals(method)
                && "configs/pull".equals(relative)) {
            return queryScope(request, "SDK", "CONFIG_PULL", true);
        }
        if ("POST".equals(method)
                && "publish/ack".equals(relative)) {
            return bodyScope("SDK", "PUBLISH_ACK", body);
        }
        if (relative.startsWith("registry/")) {
            return registryScope(request, method, relative, body);
        }
        if (relative.startsWith("management/")) {
            return managementScope(request, method, relative);
        }
        return null;
    }

    private RequestScope registryScope(
            HttpServletRequest request,
            String method,
            String relative,
            JsonNode body) {
        if ("POST".equals(method)
                && "registry/instances/register".equals(relative)) {
            return serviceBodyScope(
                    "REGISTRY_REGISTER",
                    body
            );
        }
        if ("POST".equals(method)
                && "registry/instances/heartbeat".equals(relative)) {
            return serviceBodyScope(
                    "REGISTRY_HEARTBEAT",
                    body
            );
        }
        if ("POST".equals(method)
                && "registry/instances/deregister".equals(relative)) {
            return serviceBodyScope(
                    "REGISTRY_DEREGISTER",
                    body
            );
        }
        if ("GET".equals(method)
                && ("registry/instances".equals(relative)
                || "registry/services".equals(relative))) {
            return optionalQueryScope(
                    request,
                    "REGISTRY",
                    "REGISTRY_READ"
            );
        }
        return null;
    }

    private RequestScope managementScope(
            HttpServletRequest request,
            String method,
            String relative) {
        String[] segments = relative.split("/");
        if (segments.length >= 5
                && "management".equals(segments[0])
                && "configs".equals(segments[1])) {
            String operation;
            if (segments.length == 5 && "GET".equals(method)) {
                operation = "MANAGEMENT_CONFIG_READ";
            } else if (segments.length == 5
                    && ("PUT".equals(method) || "DELETE".equals(method))) {
                operation = "MANAGEMENT_CONFIG_WRITE";
            } else if (segments.length == 6
                    && "publish".equals(segments[5])
                    && "POST".equals(method)) {
                operation = "MANAGEMENT_PUBLISH";
            } else {
                return null;
            }
            return requiredScope(
                    "MANAGEMENT",
                    operation,
                    decode(segments[4]),
                    decode(segments[3]),
                    decode(segments[2]),
                    true
            );
        }
        if (segments.length == 3
                && "publish-tasks".equals(segments[1])
                && "GET".equals(method)) {
            return unscoped("MANAGEMENT", "MANAGEMENT_TASK_READ");
        }
        if (segments.length == 4
                && "publish-tasks".equals(segments[1])
                && "retry".equals(segments[3])
                && "POST".equals(method)) {
            return unscoped("MANAGEMENT", "MANAGEMENT_TASK_RETRY");
        }
        if (segments.length == 2
                && "instances".equals(segments[1])
                && "GET".equals(method)) {
            return queryScope(
                    request,
                    "MANAGEMENT",
                    "MANAGEMENT_INSTANCE_READ",
                    true
            );
        }
        if (segments.length == 2
                && "scope-bindings".equals(segments[1])
                && "GET".equals(method)) {
            return optionalQueryScope(
                    request,
                    "MANAGEMENT",
                    "MANAGEMENT_SCOPE_READ"
            );
        }
        if (segments.length == 3
                && "registry".equals(segments[1])
                && ("services".equals(segments[2])
                || "instances".equals(segments[2]))
                && "GET".equals(method)) {
            return optionalQueryScope(
                    request,
                    "MANAGEMENT",
                    "MANAGEMENT_REGISTRY_READ"
            );
        }
        return null;
    }

    private RequestScope bodyScope(
            String clientType,
            String operation,
            JsonNode body) {
        return requiredScope(
                clientType,
                operation,
                text(body, "appCode"),
                text(body, "env"),
                text(body, "bizCode"),
                true
        );
    }

    private RequestScope serviceBodyScope(
            String operation,
            JsonNode body) {
        JsonNode serviceKey = body.path("serviceKey");
        return requiredScope(
                "REGISTRY",
                operation,
                firstText(serviceKey, body, "appCode"),
                firstText(serviceKey, body, "env"),
                firstText(serviceKey, body, "bizCode"),
                true
        );
    }

    private RequestScope queryScope(
            HttpServletRequest request,
            String clientType,
            String operation,
            boolean includeAppCode) {
        return requiredScope(
                clientType,
                operation,
                includeAppCode
                        ? request.getParameter("appCode")
                        : null,
                request.getParameter("env"),
                request.getParameter("bizCode"),
                includeAppCode
        );
    }

    private RequestScope optionalQueryScope(
            HttpServletRequest request,
            String clientType,
            String operation) {
        return new RequestScope(
                clientType,
                operation,
                request.getParameter("appCode"),
                request.getParameter("env"),
                request.getParameter("bizCode")
        );
    }

    private RequestScope requiredScope(
            String clientType,
            String operation,
            String appCode,
            String env,
            String bizCode,
            boolean appCodeRequired) {
        if (appCodeRequired && !hasText(appCode)
                || !hasText(env)
                || !hasText(bizCode)) {
            return null;
        }
        return new RequestScope(
                clientType,
                operation,
                appCode,
                env,
                bizCode
        );
    }

    private RequestScope unscoped(String clientType, String operation) {
        return new RequestScope(clientType, operation, null, null, null);
    }

    private JsonNode body(byte[] bytes) {
        if (bytes.length == 0) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(bytes);
        } catch (IOException invalidBody) {
            return objectMapper.createObjectNode();
        }
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return value == null || value.isBlank() ? null : value;
    }

    private String firstText(
            JsonNode preferred,
            JsonNode fallback,
            String field) {
        String value = text(preferred, field);
        return value == null ? text(fallback, field) : value;
    }

    private String decode(String value) {
        return UriUtils.decode(value, StandardCharsets.UTF_8);
    }

    private void attachPrincipal(
            HttpServletRequest request,
            DdcServicePrincipal principal) {
        request.setAttribute(DdcServicePrincipal.REQUEST_ATTRIBUTE, principal);
        var authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_DDC_SERVICE"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
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
        objectMapper.writeValue(response.getOutputStream(), ResultRecord.failure(status));
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

    private void rejectScope(HttpServletResponse response)
            throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "code", "DDC_HMAC_SCOPE_DENIED",
                "message", "DDC HMAC credential scope denied"
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

    private record RequestScope(
            String clientType,
            String operation,
            String appCode,
            String env,
            String bizCode) {
    }
}
