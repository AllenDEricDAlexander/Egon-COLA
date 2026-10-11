package top.egon.cola.platform.tianquan.shoubing.yuheng.security;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader;
import top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedPrincipalConverter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.LegacyIssuerCutoverPolicy;
import top.egon.cola.component.yuheng.core.security.AuthenticationFailure;
import top.egon.cola.component.yuheng.core.security.GatewayAuthContext;
import top.egon.cola.platform.tianquan.shoubing.contract.IdpPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.domain.GatewayVerifiedIdentityBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpJwtVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerStateReader;

import java.time.Clock;
import java.util.Objects;
import java.util.Set;

/**
 * Adapts the shared USER/SERVICE verifier to the Gateway authentication SPI.
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class IdpGatewayJwtVerifier
        implements IdpIdentityAuthenticationProvider.TokenVerifier {

    @Qualifier("idpGatewayJwtDecoder")
    private final JwtDecoder decoder;
    @Qualifier("idpJwtVerifier")
    private final IdpJwtVerifier userVerifier;
    @Qualifier("idpGatewayResourceServerStateReader")
    private final IdentityResourceServerStateReader resourceStates;
    @Qualifier("idpGatewayOAuthClientStateReader")
    private final IdentityOAuthClientStateReader clientStates;
    @Qualifier("gatewayResourceServerResolver")
    private final GatewayResourceServerResolver resources;
    private final String platformAudience;
    @Qualifier("idpClock")
    private final Clock clock;
    @Qualifier("tenantIssuerAuthenticationManagerComponent")
    private final TenantIssuerAuthenticationManagerComponent issuerManager;
    @Qualifier("oauthRevocationReader")
    private final OAuthRevocationReader revocationReader;
    @Qualifier("verifiedPrincipalConverter")
    private final VerifiedPrincipalConverter principalConverter;
    @Qualifier("legacyIssuerCutoverPolicy")
    private final LegacyIssuerCutoverPolicy legacyIssuerCutoverPolicy;
    @Qualifier("idpObservedVerificationComponent")
    private final IdpObservedVerificationComponent observations;

    public IdpGatewayJwtVerifier(
            JwtDecoder decoder,
            IdentityResourceServerStateReader resourceStates,
            IdentityOAuthClientStateReader clientStates,
            GatewayResourceServerResolver resources,
            String platformAudience,
            Clock clock) {
        this(Objects.requireNonNull(decoder, "decoder"), new IdpJwtVerifier(decoder, platformAudience, clock),
                Objects.requireNonNull(resourceStates, "resourceStates"),
                Objects.requireNonNull(clientStates, "clientStates"), Objects.requireNonNull(resources, "resources"),
                required(platformAudience, "platformAudience"), Objects.requireNonNull(clock, "clock"),
                null, null, null, null, null);
    }

    /** Uses the shared issuer/claim/revocation pipeline with the Resource selected from a trusted route. */
    public static IdpGatewayJwtVerifier createConfigured(JwtDecoder legacyDecoder,
            IdentityResourceServerStateReader resourceStates, IdentityOAuthClientStateReader clientStates,
            GatewayResourceServerResolver resources, String platformAudience, Clock clock,
            TenantIssuerAuthenticationManagerComponent issuerManager, OAuthRevocationReader revocationReader,
            VerifiedPrincipalConverter principalConverter, LegacyIssuerCutoverPolicy legacyIssuerCutoverPolicy) {
        return createConfigured(legacyDecoder, resourceStates, clientStates, resources, platformAudience, clock,
                issuerManager, revocationReader, principalConverter, legacyIssuerCutoverPolicy, null);
    }

    @Override
    public Verification verify(GatewayAuthContext context, String token) {
        if (issuerManager != null) {
            return verifyConfigured(context, token);
        }
        AccessTokenVerification<?> user = userVerifier.verifyUser(token);
        if (user instanceof AccessTokenVerification.Valid<?> valid) {
            return Verification.valid(valid.principal(), valid.scopes(), valid.proof());
        }
        if (user instanceof AccessTokenVerification.Expired<?>) {
            return Verification.expired("JWT_EXPIRED");
        }
        String reason = ((AccessTokenVerification.Invalid<?>) user).reasonCode();
        if (!"JWT_PRINCIPAL_TYPE_INVALID".equals(reason)) {
            return Verification.invalid(reason);
        }
        try {
            var resource = resources.resolve(context.attributes());
            IdpJwtVerifier serviceVerifier = new IdpJwtVerifier(
                    decoder,
                    resourceStates,
                    clientStates,
                    resource.resourceServerId(),
                    resource.resourceUri(),
                    platformAudience,
                    clock);
            AccessTokenVerification<?> service = serviceVerifier.verifyService(token);
            if (service instanceof AccessTokenVerification.Valid<?> valid) {
                return Verification.valid(valid.principal(), valid.scopes(), valid.proof());
            }
            if (service instanceof AccessTokenVerification.Expired<?>) {
                return Verification.expired("JWT_EXPIRED");
            }
            return Verification.invalid(((AccessTokenVerification.Invalid<?>) service)
                    .reasonCode());
        } catch (GatewayResourceServerResolver.ResourceResolutionException invalid) {
            return Verification.invalid(invalid.getMessage());
        }
    }

    public static IdpGatewayJwtVerifier createConfigured(JwtDecoder legacyDecoder,
            IdentityResourceServerStateReader resourceStates, IdentityOAuthClientStateReader clientStates,
            GatewayResourceServerResolver resources, String platformAudience, Clock clock,
            TenantIssuerAuthenticationManagerComponent issuerManager, OAuthRevocationReader revocationReader,
            VerifiedPrincipalConverter principalConverter, LegacyIssuerCutoverPolicy legacyIssuerCutoverPolicy,
            IdpObservedVerificationComponent observations) {
        Objects.requireNonNull(issuerManager, "issuerManager");
        if (!issuerManager.isConfigured()) { throw new IllegalStateException("GATEWAY_TRUSTED_ISSUERS_REQUIRED"); }
        Objects.requireNonNull(revocationReader, "revocationReader");
        Objects.requireNonNull(principalConverter, "principalConverter");
        return new IdpGatewayJwtVerifier(legacyDecoder, null,
                Objects.requireNonNull(resourceStates, "resourceStates"),
                Objects.requireNonNull(clientStates, "clientStates"), Objects.requireNonNull(resources, "resources"),
                required(platformAudience, "platformAudience"), Objects.requireNonNull(clock, "clock"),
                issuerManager, revocationReader, principalConverter, legacyIssuerCutoverPolicy, observations);
    }

    private Verification verifyConfigured(GatewayAuthContext context, String token) {
        if (!issuerManager.isConfigured()) {
            throw new IllegalStateException("GATEWAY_TRUSTED_ISSUERS_UNAVAILABLE");
        }
        var resource = resources.resolve(Objects.requireNonNull(context, "context").attributes());
        IdpJwtVerifier verifier = IdpJwtVerifier.createConfigured(
                legacyIssuerCutoverPolicy == null ? null : decoder, resourceStates, clientStates,
                resource.resourceServerId(), resource.resourceUri(), platformAudience, clock,
                issuerManager, revocationReader, principalConverter, legacyIssuerCutoverPolicy);
        AccessTokenVerification<IdpPrincipal> result = observations == null ? verifier.verifyAny(token)
                : observations.observe(capture -> verifier.verifyAny(token, capture), resource.resourceUri(), context.traceId());
        if (result instanceof AccessTokenVerification.Valid<IdpPrincipal> valid) {
            return Verification.valid(valid.principal(), valid.scopes(), valid.proof());
        }
        if (result instanceof AccessTokenVerification.Expired<?>) {
            return Verification.expired("JWT_EXPIRED");
        }
        if (result instanceof AccessTokenVerification.Unavailable<?>) {
            // The existing authentication provider translates dependency errors to a fail-closed 503.
            throw new IllegalStateException("OAUTH_SECURITY_STATE_UNAVAILABLE");
        }
        return Verification.invalid(((AccessTokenVerification.Invalid<IdpPrincipal>) result).reasonCode());
    }

    public record Verification(
            IdpPrincipal principal,
            AuthenticationFailure failure,
            String reason,
            @com.fasterxml.jackson.annotation.JsonIgnore Set<String> scopes,
            @com.fasterxml.jackson.annotation.JsonIgnore @jakarta.validation.Valid
            TokenAuditProofBO proof) {

        public Verification {
            scopes = new GatewayVerifiedIdentityBO(proof, scopes).verifiedScopes();
        }

        public Verification(IdpPrincipal principal, AuthenticationFailure failure, String reason) {
            this(principal, failure, reason,
                    principal instanceof ServiceIdentityPrincipal service ? service.scopes() : Set.of(), null);
        }

        static Verification valid(IdpPrincipal principal) {
            return new Verification(
                    Objects.requireNonNull(principal, "principal"),
                    AuthenticationFailure.NONE,
                    null);
        }

        static Verification valid(IdpPrincipal principal, Set<String> scopes, TokenAuditProofBO proof) {
            return new Verification(Objects.requireNonNull(principal, "principal"), AuthenticationFailure.NONE, null, scopes, proof);
        }

        static Verification expired(String reason) {
            return new Verification(null, AuthenticationFailure.EXPIRED, reason);
        }

        static Verification invalid(String reason) {
            return new Verification(null, AuthenticationFailure.INVALID, reason);
        }

        @Override public String toString() { return "Verification[failure=" + failure + ", reason=" + reason + "]"; }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
