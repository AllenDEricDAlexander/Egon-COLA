package top.egon.cola.platform.idp.gateway.security;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import top.egon.cola.component.gateway.core.security.AuthenticationFailure;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.platform.idp.contract.IdpPrincipal;
import top.egon.cola.platform.idp.starter.security.AccessTokenVerification;
import top.egon.cola.platform.idp.starter.security.IdpJwtVerifier;
import top.egon.cola.platform.idp.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerStateReader;

import java.time.Clock;
import java.util.Objects;

/**
 * Adapts the shared USER/SERVICE verifier to the Gateway authentication SPI.
 */
public final class IdpGatewayJwtVerifier
        implements IdpIdentityAuthenticationProvider.TokenVerifier {

    private final JwtDecoder decoder;
    private final IdpJwtVerifier userVerifier;
    private final IdentityResourceServerStateReader resourceStates;
    private final IdentityOAuthClientStateReader clientStates;
    private final GatewayResourceServerResolver resources;
    private final String platformAudience;
    private final Clock clock;

    public IdpGatewayJwtVerifier(
            JwtDecoder decoder,
            IdentityResourceServerStateReader resourceStates,
            IdentityOAuthClientStateReader clientStates,
            GatewayResourceServerResolver resources,
            String platformAudience,
            Clock clock) {
        this.decoder = Objects.requireNonNull(decoder, "decoder");
        this.userVerifier = new IdpJwtVerifier(
                decoder,
                platformAudience,
                clock);
        this.resourceStates = Objects.requireNonNull(resourceStates, "resourceStates");
        this.clientStates = Objects.requireNonNull(clientStates, "clientStates");
        this.resources = Objects.requireNonNull(resources, "resources");
        this.platformAudience = required(platformAudience, "platformAudience");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Verification verify(GatewayAuthContext context, String token) {
        AccessTokenVerification<?> user = userVerifier.verifyUser(token);
        if (user instanceof AccessTokenVerification.Valid<?> valid) {
            return Verification.valid(valid.principal());
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
                return Verification.valid(valid.principal());
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

    public record Verification(
            IdpPrincipal principal,
            AuthenticationFailure failure,
            String reason) {

        static Verification valid(IdpPrincipal principal) {
            return new Verification(
                    Objects.requireNonNull(principal, "principal"),
                    AuthenticationFailure.NONE,
                    null);
        }

        static Verification expired(String reason) {
            return new Verification(null, AuthenticationFailure.EXPIRED, reason);
        }

        static Verification invalid(String reason) {
            return new Verification(null, AuthenticationFailure.INVALID, reason);
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
