package top.egon.cola.platform.idp.core.oauth;

import top.egon.cola.platform.idp.core.port.AuthorizationCodeStore;
import top.egon.cola.platform.idp.core.port.OAuthClientStore;
import top.egon.cola.platform.idp.core.port.TenantMembershipPort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class AuthorizationFacade {

    private static final Duration CODE_TTL = Duration.ofSeconds(60);
    private static final Pattern CODE_VERIFIER = Pattern.compile(
            "[A-Za-z0-9\\-._~]{43,128}"
    );
    private static final Pattern CODE_CHALLENGE = Pattern.compile(
            "[A-Za-z0-9_-]{43}"
    );
    private static final int MAXIMUM_BROWSER_VALUE_LENGTH = 512;

    private final OAuthClientStore clients;
    private final AuthorizationCodeStore codes;
    private final TenantMembershipPort memberships;
    private final Clock clock;
    private final Supplier<String> codeGenerator;

    public AuthorizationFacade(
            OAuthClientStore clients,
            AuthorizationCodeStore codes,
            TenantMembershipPort memberships,
            Clock clock
    ) {
        this(clients, codes, memberships, clock, secureCodeGenerator());
    }

    public AuthorizationFacade(
            OAuthClientStore clients,
            AuthorizationCodeStore codes,
            TenantMembershipPort memberships,
            Clock clock,
            Supplier<String> codeGenerator
    ) {
        this.clients = Objects.requireNonNull(clients, "clients");
        this.codes = Objects.requireNonNull(codes, "codes");
        this.memberships = Objects.requireNonNull(
                memberships,
                "memberships"
        );
        this.clock = Objects.requireNonNull(clock, "clock");
        this.codeGenerator = Objects.requireNonNull(
                codeGenerator,
                "codeGenerator"
        );
    }

    public AuthorizationResult authorize(
            AuthorizationRequest request,
            String identitySub
    ) {
        Objects.requireNonNull(request, "request");
        String subject = required(identitySub, "identitySub");
        validateBrowserRequest(request);
        OAuthClient client = clients.findById(request.clientId())
                .orElseThrow(() -> oauth(
                        "unauthorized_client",
                        "client is not authorized"
                ));
        validateClient(client, request);
        TenantMembershipPort.TenantMembership membership = membership(
                subject,
                request.tenantId(),
                request.clientId()
        );
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(CODE_TTL);
        AuthorizationCode authorizationCode = new AuthorizationCode(
                subject,
                request.tenantId(),
                membership.rbac3UserId(),
                request.clientId(),
                request.audience(),
                request.redirectUri(),
                request.nonce(),
                request.codeChallenge(),
                issuedAt,
                expiresAt
        );
        String rawCode = required(codeGenerator.get(), "authorizationCode");
        if (rawCode.length() < 32) {
            throw new IllegalStateException(
                    "authorization code generator returned a short value"
            );
        }
        codes.put(digest(rawCode), authorizationCode, CODE_TTL);
        return new AuthorizationResult(
                rawCode,
                request.state(),
                request.redirectUri(),
                expiresAt
        );
    }

    public AuthorizationCode consume(
            String rawCode,
            String codeVerifier,
            String redirectUri,
            String clientId
    ) {
        String code = grantValue(rawCode);
        String verifier = grantValue(codeVerifier);
        if (!CODE_VERIFIER.matcher(verifier).matches()) {
            throw invalidGrant();
        }
        String redirect = grantValue(redirectUri);
        String client = grantValue(clientId);
        AuthorizationCode authorizationCode = codes.consume(digest(code));
        if (authorizationCode == null
                || !authorizationCode.expiresAt().isAfter(clock.instant())
                || !constantTimeEquals(
                        authorizationCode.codeChallenge(),
                        s256(verifier)
                )
                || !authorizationCode.redirectUri().equals(redirect)
                || !authorizationCode.clientId().equals(client)) {
            throw invalidGrant();
        }
        return authorizationCode;
    }

    private void validateBrowserRequest(AuthorizationRequest request) {
        if (!"code".equals(request.responseType())) {
            throw oauth("invalid_request", "response_type must be code");
        }
        requiredRequestValue(request.clientId(), "client_id");
        requiredRequestValue(request.redirectUri(), "redirect_uri");
        requiredRequestValue(request.audience(), "audience");
        requiredRequestValue(request.tenantId(), "tenant_id");
        browserValue(request.state(), "state");
        browserValue(request.nonce(), "nonce");
        if (!"S256".equals(request.codeChallengeMethod())) {
            throw oauth(
                    "invalid_request",
                    "code_challenge_method must be S256"
            );
        }
        if (request.codeChallenge() == null
                || !CODE_CHALLENGE.matcher(
                request.codeChallenge()
        ).matches()) {
            throw oauth("invalid_request", "invalid code_challenge");
        }
    }

    private void validateClient(
            OAuthClient client,
            AuthorizationRequest request
    ) {
        if (client.status() != OAuthClient.Status.ACTIVE) {
            throw oauth(
                    "unauthorized_client",
                    "client is not authorized"
            );
        }
        if (!client.pkceRequired()
                || !client.acceptsRedirectUri(request.redirectUri())) {
            throw oauth(
                    "invalid_request",
                    "redirect URI or PKCE policy is invalid"
            );
        }
        if (!client.acceptsAudience(request.audience())) {
            throw oauth(
                    "invalid_target",
                    "requested audience is not registered"
            );
        }
    }

    private TenantMembershipPort.TenantMembership membership(
            String identitySub,
            String tenantId,
            String clientId
    ) {
        try {
            TenantMembershipPort.TenantMembership membership =
                    memberships.resolve(identitySub, tenantId, clientId);
            if (membership == null
                    || membership.status()
                    != TenantMembershipPort.MembershipStatus.ACTIVE
                    || !identitySub.equals(membership.identitySub())
                    || !tenantId.equals(membership.tenantId())
                    || membership.rbac3UserId() == null
                    || membership.rbac3UserId().isBlank()) {
                throw oauth(
                        "access_denied",
                        "tenant membership is not active"
                );
            }
            return membership;
        } catch (TenantMembershipPort.TenantMembershipException exception) {
            throw oauth(
                    "access_denied",
                    "tenant membership is not active"
            );
        }
    }

    private void browserValue(String value, String field) {
        requiredRequestValue(value, field);
        if (value.length() > MAXIMUM_BROWSER_VALUE_LENGTH) {
            throw oauth("invalid_request", field + " is too long");
        }
    }

    private String requiredRequestValue(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw oauth("invalid_request", field + " is required");
        }
        return value;
    }

    private String grantValue(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw invalidGrant();
        }
        return value;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static String digest(String value) {
        return base64Url(sha256(value));
    }

    private static String s256(String verifier) {
        return base64Url(sha256(verifier));
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(
                    value.getBytes(StandardCharsets.US_ASCII)
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.US_ASCII),
                right.getBytes(StandardCharsets.US_ASCII)
        );
    }

    private static OAuthException invalidGrant() {
        return oauth("invalid_grant", "authorization grant is invalid");
    }

    private static OAuthException oauth(String error, String message) {
        return new OAuthException(error, message);
    }

    private static Supplier<String> secureCodeGenerator() {
        SecureRandom random = new SecureRandom();
        return () -> {
            byte[] value = new byte[32];
            random.nextBytes(value);
            return base64Url(value);
        };
    }

    public record AuthorizationResult(
            String code,
            String state,
            String redirectUri,
            Instant expiresAt
    ) {
    }
}
