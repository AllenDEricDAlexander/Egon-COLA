package top.egon.cola.component.gateway.admin.interfaces.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.component.ddc.security.DdcCanonicalRequest;
import top.egon.cola.component.ddc.security.DdcRequestSigner;
import top.egon.cola.component.gateway.admin.application.credential.GatewayCredentialStore;
import top.egon.cola.component.gateway.admin.application.credential.GatewaySecretProtector;
import top.egon.cola.component.gateway.admin.application.reporting.GatewayHmacNonceStore;
import top.egon.cola.component.gateway.admin.application.reporting.GatewayReportAuthentication;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayApplicationEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayApplicationRepository;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GatewayReportHmacFilter extends OncePerRequestFilter {

    private static final String PREFIX =
            "/api/v1/gateway/openapi/interface-definitions/";

    private static final int MAX_BODY_BYTES = 8 * 1024 * 1024;

    private final GatewayCredentialStore credentials;

    private final GatewayApplicationRepository applications;

    private final GatewayHmacNonceStore nonces;

    private final GatewaySecretProtector protector;

    private final ObjectMapper objectMapper;

    private final Duration allowedSkew;

    private final Clock clock;

    private final DdcRequestSigner signer = new DdcRequestSigner();

    @Autowired
    public GatewayReportHmacFilter(
            GatewayCredentialStore credentials,
            GatewayApplicationRepository applications,
            GatewayHmacNonceStore nonces,
            ObjectProvider<GatewaySecretProtector> protector,
            ObjectMapper objectMapper,
            @Value("${gateway.admin.hmac.allowed-skew:PT5M}")
            Duration allowedSkew) {
        this(
                credentials,
                applications,
                nonces,
                protector.getIfAvailable(),
                objectMapper,
                allowedSkew,
                Clock.systemUTC()
        );
    }

    GatewayReportHmacFilter(
            GatewayCredentialStore credentials,
            GatewayApplicationRepository applications,
            GatewayHmacNonceStore nonces,
            GatewaySecretProtector protector,
            ObjectMapper objectMapper,
            Duration allowedSkew,
            Clock clock) {
        this.credentials = credentials;
        this.applications = applications;
        this.nonces = nonces;
        this.protector = protector;
        this.objectMapper = objectMapper;
        this.allowedSkew = allowedSkew;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        try {
            CachedBodyRequest cached = new CachedBodyRequest(request);
            if (cached.body.length > MAX_BODY_BYTES) {
                throw new AuthenticationFailure(
                        413,
                        "GATEWAY_REPORT_BODY_TOO_LARGE"
                );
            }
            GatewayReportAuthentication authentication =
                    authenticate(cached);
            cached.setAttribute(
                    GatewayReportAuthentication.REQUEST_ATTRIBUTE,
                    authentication
            );
            chain.doFilter(cached, response);
        } catch (AuthenticationFailure failure) {
            response.setStatus(failure.status);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(
                    response.getOutputStream(),
                    Map.of(
                            "code", failure.code,
                            "message", failure.getMessage(),
                            "timestamp", clock.instant()
                    )
            );
        }
    }

    private GatewayReportAuthentication authenticate(
            CachedBodyRequest request) {
        if (request.getHeader("X-Admin-Actor-Id") != null) {
            throw failure("GATEWAY_REPORT_CREDENTIAL_REQUIRED");
        }
        String accessKey = header(
                request,
                DdcRequestSigner.ACCESS_KEY_HEADER
        );
        long timestamp = timestamp(request);
        Instant now = clock.instant();
        Instant signedAt = Instant.ofEpochMilli(timestamp);
        if (Duration.between(signedAt, now).abs().compareTo(allowedSkew) > 0) {
            throw failure("GATEWAY_REPORT_TIMESTAMP_INVALID");
        }
        GatewayCredentialStore.CredentialRecord credential =
                credentials.findByAccessKey(accessKey)
                        .filter(value -> active(value, now))
                        .orElseThrow(() ->
                                failure("GATEWAY_REPORT_CREDENTIAL_INVALID"));
        GatewayApplicationEntity application =
                applications.findByIdAndDeletedFalse(
                                credential.applicationId()
                        )
                        .orElseThrow(() ->
                                failure("GATEWAY_REPORT_SCOPE_INVALID"));
        String requestedApplication = header(
                request,
                "X-Gateway-Application-Code"
        );
        if (!application.getApplicationCode().equals(
                requestedApplication
        )) {
            throw failure("GATEWAY_REPORT_SCOPE_INVALID");
        }
        if (protector == null) {
            throw new AuthenticationFailure(
                    503,
                    "GATEWAY_REPORT_SECRET_PROTECTOR_UNAVAILABLE"
            );
        }
        String nonce = header(request, DdcRequestSigner.NONCE_HEADER);
        DdcCanonicalRequest canonical = new DdcCanonicalRequest(
                request.getMethod(),
                request.getRequestURI(),
                query(request),
                timestamp,
                nonce,
                request.body
        );
        if (!signer.matches(
                canonical.contentSha256(),
                header(request, DdcRequestSigner.CONTENT_SHA256_HEADER)
        )) {
            throw failure("GATEWAY_REPORT_BODY_DIGEST_INVALID");
        }
        String secret;
        try {
            secret = protector.unprotect(
                    new GatewaySecretProtector.ProtectedSecret(
                            credential.secretCiphertext(),
                            credential.keyVersion()
                    ),
                    credential.applicationId() + ":" + accessKey
            );
        } catch (RuntimeException unavailable) {
            throw failure("GATEWAY_REPORT_CREDENTIAL_INVALID");
        }
        if (!signer.matches(
                signer.sign(canonical, secret),
                header(request, DdcRequestSigner.SIGNATURE_HEADER)
        )) {
            throw failure("GATEWAY_REPORT_SIGNATURE_INVALID");
        }
        if (!nonces.claim(
                accessKey,
                nonce,
                now.plus(allowedSkew.multipliedBy(2)),
                now
        )) {
            throw new AuthenticationFailure(
                    409,
                    "GATEWAY_REPORT_REPLAYED"
            );
        }
        return new GatewayReportAuthentication(
                application.getId(),
                application.getApplicationCode(),
                application.getEnv(),
                application.getNamespace(),
                accessKey
        );
    }

    private boolean active(
            GatewayCredentialStore.CredentialRecord credential,
            Instant now) {
        return !"REVOKED".equals(credential.status())
                && !now.isBefore(credential.validFrom())
                && (credential.validUntil() == null
                || now.isBefore(credential.validUntil()));
    }

    private long timestamp(HttpServletRequest request) {
        try {
            return Long.parseLong(header(
                    request,
                    DdcRequestSigner.TIMESTAMP_HEADER
            ));
        } catch (NumberFormatException invalid) {
            throw failure("GATEWAY_REPORT_TIMESTAMP_INVALID");
        }
    }

    private String header(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            throw failure("GATEWAY_REPORT_HEADER_MISSING");
        }
        return value.trim();
    }

    private Map<String, List<String>> query(HttpServletRequest request) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, items) ->
                values.put(key, Arrays.asList(items)));
        return values;
    }

    private AuthenticationFailure failure(String code) {
        return new AuthenticationFailure(401, code);
    }

    private static final class AuthenticationFailure
            extends RuntimeException {

        private final int status;

        private final String code;

        private AuthenticationFailure(int status, String code) {
            super("gateway report authentication failed");
            this.status = status;
            this.code = code;
        }
    }

    private static final class CachedBodyRequest
            extends HttpServletRequestWrapper {

        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request)
                throws IOException {
            super(request);
            body = request.getInputStream().readAllBytes();
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }

                @Override
                public int read() {
                    return input.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(
                    getInputStream(),
                    StandardCharsets.UTF_8
            ));
        }
    }
}
