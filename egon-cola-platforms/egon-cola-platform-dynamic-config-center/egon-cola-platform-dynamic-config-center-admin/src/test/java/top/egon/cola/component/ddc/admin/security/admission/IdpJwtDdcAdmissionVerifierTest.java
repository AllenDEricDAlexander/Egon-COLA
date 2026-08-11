package top.egon.cola.component.ddc.admin.security.admission;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import top.egon.cola.platform.idp.core.resource.ResourceServerStatus;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerState;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerStateReader;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdpJwtDdcAdmissionVerifierTest {

    private static final Instant NOW = Instant.parse("2026-08-11T08:00:00Z");

    private static final String ISSUER = "https://idp.example";

    private static final String RESOURCE_SERVER_ID = "permission-idp-prod";

    @Test
    void acceptsAnActiveCurrentResourceAndReturnsOnlyAuditClaims() {
        DdcAdmissionClaims claims = verifier(jwt(), activeProjection())
                .verify("signed-ticket", "permission", "idp", "prod", "idp-1");

        assertThat(claims).isEqualTo(new DdcAdmissionClaims(
                RESOURCE_SERVER_ID,
                "https://api.example/idp",
                7L,
                "permission",
                "idp",
                "prod",
                "idp-1",
                "credential-1",
                NOW.minusSeconds(5),
                NOW.plusSeconds(40)
        ));
        assertThat(claims.toString()).doesNotContain("signed-ticket");
    }

    @Test
    void acceptsTheInitialResourceVersionZero() {
        Jwt initialTicket = jwt(
                Map.of(),
                Map.of("resource_version", 0L)
        );
        IdentityResourceServerState initialProjection = resourceState(
                ResourceServerStatus.ACTIVE,
                0L,
                "idp"
        );

        DdcAdmissionClaims claims = verifier(
                initialTicket,
                initialProjection
        ).verify(
                "signed-ticket",
                "permission",
                "idp",
                "prod",
                "idp-1"
        );

        assertThat(claims.resourceVersion()).isZero();
    }

    @Test
    void rejectsMissingTicketAndDecoderSignatureFailure() {
        IdpJwtDdcAdmissionVerifier verifier = verifier(jwt(), activeProjection());

        assertAdmissionFailure(
                () -> verifier.verify(" ", "permission", "idp", "prod", "idp-1"),
                "DDC_RESOURCE_ADMISSION_REQUIRED"
        );

        JwtDecoder rejectingDecoder = token -> {
            throw new JwtException("signature detail must not escape");
        };
        assertAdmissionFailure(
                () -> verifier(rejectingDecoder, activeProjection()).verify(
                        "untrusted", "permission", "idp", "prod", "idp-1"),
                "DDC_RESOURCE_ADMISSION_INVALID"
        );
    }

    @Test
    void rejectsWrongJwtTypeUseIssuerAudienceAndTime() {
        assertInvalid(jwt(Map.of("typ", "at+jwt"), Map.of()));
        assertInvalid(jwt(Map.of(), Map.of("token_use", "access")));
        assertInvalid(jwt(Map.of(), Map.of("iss", "https://other.example")));
        assertInvalid(jwt(Map.of(), Map.of("aud", List.of("other"))));
        assertInvalid(jwt(Map.of(), Map.of("aud", List.of("ddc-registry", "other"))));
        assertInvalid(jwt(Map.of(), Map.of("nbf", NOW.plusSeconds(1))));
        assertInvalid(jwt(Map.of(), Map.of("iat", NOW.plusSeconds(1))));

        Jwt expired = jwt(Map.of(), Map.of("exp", NOW));
        assertAdmissionFailure(
                () -> verifier(expired, activeProjection()).verify(
                        "ticket", "permission", "idp", "prod", "idp-1"),
                "DDC_RESOURCE_ADMISSION_EXPIRED"
        );
    }

    @Test
    void rejectsEveryRequestBindingMismatch() {
        IdpJwtDdcAdmissionVerifier verifier = verifier(jwt(), activeProjection());

        assertBindingMismatch(verifier, "other", "idp", "prod", "idp-1");
        assertBindingMismatch(verifier, "permission", "other", "prod", "idp-1");
        assertBindingMismatch(verifier, "permission", "idp", "dev", "idp-1");
        assertBindingMismatch(verifier, "permission", "idp", "prod", "idp-2");
    }

    @Test
    void rejectsDisabledMissingMalformedAndStaleResourceProjection() {
        assertProjectionInvalid(null);
        assertAdmissionFailure(
                () -> verifier(jwt(), resourceServerId -> {
                    throw new IllegalStateException("malformed projection");
                }).verify("ticket", "permission", "idp", "prod", "idp-1"),
                "DDC_RESOURCE_ADMISSION_INVALID"
        );
        assertProjectionInvalid(resourceState(
                ResourceServerStatus.DISABLED, 7L, "idp"));
        assertProjectionInvalid(resourceState(
                ResourceServerStatus.ACTIVE, 8L, "idp"));
        assertProjectionInvalid(resourceState(
                ResourceServerStatus.ACTIVE, 7L, "rbac"));
    }

    private void assertInvalid(Jwt jwt) {
        assertAdmissionFailure(
                () -> verifier(jwt, activeProjection()).verify(
                        "ticket", "permission", "idp", "prod", "idp-1"),
                "DDC_RESOURCE_ADMISSION_INVALID"
        );
    }

    private void assertProjectionInvalid(
            IdentityResourceServerState projection
    ) {
        assertAdmissionFailure(
                () -> verifier(jwt(), projection).verify(
                        "ticket", "permission", "idp", "prod", "idp-1"),
                "DDC_RESOURCE_ADMISSION_INVALID"
        );
    }

    private void assertBindingMismatch(
            IdpJwtDdcAdmissionVerifier verifier,
            String bizCode,
            String appCode,
            String env,
            String instanceId
    ) {
        assertAdmissionFailure(
                () -> verifier.verify("ticket", bizCode, appCode, env, instanceId),
                "DDC_RESOURCE_ADMISSION_BINDING_MISMATCH"
        );
    }

    private void assertAdmissionFailure(Runnable operation, String status) {
        assertThatThrownBy(operation::run)
                .isInstanceOf(DdcAdmissionException.class)
                .extracting(failure -> ((DdcAdmissionException) failure).getStatus())
                .isEqualTo(status);
    }

    private IdpJwtDdcAdmissionVerifier verifier(
            Jwt jwt,
            IdentityResourceServerState projection
    ) {
        return verifier(token -> jwt, projection);
    }

    private IdpJwtDdcAdmissionVerifier verifier(
            JwtDecoder decoder,
            IdentityResourceServerState projection
    ) {
        return verifier(decoder, resourceServerId ->
                java.util.Optional.ofNullable(projection));
    }

    private IdpJwtDdcAdmissionVerifier verifier(
            Jwt jwt,
            IdentityResourceServerStateReader reader
    ) {
        return verifier(token -> jwt, reader);
    }

    private IdpJwtDdcAdmissionVerifier verifier(
            JwtDecoder decoder,
            IdentityResourceServerStateReader reader
    ) {
        return new IdpJwtDdcAdmissionVerifier(
                decoder,
                reader,
                ISSUER,
                "ddc-registry",
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private Jwt jwt() {
        return jwt(Map.of(), Map.of());
    }

    private Jwt jwt(Map<String, Object> headerOverrides,
                    Map<String, Object> claimOverrides) {
        Map<String, Object> headers = new java.util.LinkedHashMap<>(Map.of(
                "alg", "RS256",
                "typ", "rs-admission+jwt"
        ));
        headers.putAll(headerOverrides);
        Map<String, Object> claims = new java.util.LinkedHashMap<>();
        claims.put("iss", ISSUER);
        claims.put("sub", RESOURCE_SERVER_ID);
        claims.put("aud", List.of("ddc-registry"));
        claims.put("token_use", "resource_server_admission");
        claims.put("resource", "https://api.example/idp");
        claims.put("resource_version", 7L);
        claims.put("biz", "permission");
        claims.put("app", "idp");
        claims.put("env", "prod");
        claims.put("instance_id", "idp-1");
        claims.put("credential_id", "credential-1");
        claims.put("jti", "ticket-1");
        claims.put("iat", NOW.minusSeconds(5));
        claims.put("nbf", NOW.minusSeconds(5));
        claims.put("exp", NOW.plusSeconds(40));
        claims.putAll(claimOverrides);
        return new Jwt(
                "redacted",
                (Instant) claims.get("iat"),
                (Instant) claims.get("exp"),
                headers,
                claims
        );
    }

    private IdentityResourceServerState activeProjection() {
        return resourceState(ResourceServerStatus.ACTIVE, 7L, "idp");
    }

    private IdentityResourceServerState resourceState(
            ResourceServerStatus status,
            long version,
            String appCode
    ) {
        return new IdentityResourceServerState(
                RESOURCE_SERVER_ID,
                URI.create("https://api.example/idp"),
                "permission",
                appCode,
                "prod",
                status,
                version
        );
    }
}
