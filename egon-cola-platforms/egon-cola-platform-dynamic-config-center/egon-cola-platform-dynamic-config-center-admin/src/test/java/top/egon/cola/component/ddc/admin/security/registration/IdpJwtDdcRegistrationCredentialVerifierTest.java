package top.egon.cola.component.ddc.admin.security.registration;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.error.DdcErrorStatus;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.idp.contract.ServiceTokenContext;
import top.egon.cola.platform.idp.starter.security.AccessTokenVerification;
import top.egon.cola.platform.idp.starter.security.ServiceAccessTokenVerifier;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdpJwtDdcRegistrationCredentialVerifierTest {

    private static final Instant NOW = Instant.parse("2026-08-11T08:00:00Z");

    private static final URI RESOURCE_URI = URI.create(
            "https://api.example/idp"
    );

    @Test
    void acceptsAValidPlatformServiceTokenAndReturnsFullIdentity() {
        ServiceIdentityPrincipal principal = principal(
                ServiceTokenContext.PLATFORM,
                null,
                RESOURCE_URI,
                Set.of(IdpJwtDdcRegistrationCredentialVerifier.REGISTRATION_SCOPE),
                "permission",
                "idp",
                "prod"
        );

        VerifiedDdcRegistrationIdentity identity = verifier(
                new AccessTokenVerification.Valid<>(principal)
        ).verify("service-token", "permission", "idp", "prod", "idp-1");

        assertThat(identity.appId()).isEqualTo("app-id");
        assertThat(identity.clientId()).isEqualTo("client-id");
        assertThat(identity.resourceServerId()).isEqualTo("permission-idp-prod");
        assertThat(identity.bizCode()).isEqualTo("permission");
        assertThat(identity.appCode()).isEqualTo("idp");
        assertThat(identity.environment()).isEqualTo("prod");
        assertThat(identity.instanceId()).isEqualTo("idp-1");
        assertThat(identity.credentialId()).isEqualTo("credential-1");
        assertThat(identity.tokenId()).isEqualTo("token-1");
        assertThat(identity.toString()).doesNotContain("service-token");
    }

    @Test
    void rejectsMissingInvalidAndExpiredServiceTokens() {
        IdpJwtDdcRegistrationCredentialVerifier invalid = verifier(
                new AccessTokenVerification.Invalid<>("signature_invalid")
        );
        assertFailure(
                () -> invalid.verify(
                        "service-token", "permission", "idp", "prod", "idp-1"),
                DdcErrorStatus.RESOURCE_ADMISSION_INVALID
        );
        assertFailure(
                () -> invalid.verify(" ", "permission", "idp", "prod", "idp-1"),
                DdcErrorStatus.RESOURCE_ADMISSION_REQUIRED
        );

        IdpJwtDdcRegistrationCredentialVerifier expired = verifier(
                new AccessTokenVerification.Expired<>()
        );
        assertFailure(
                () -> expired.verify(
                        "service-token", "permission", "idp", "prod", "idp-1"),
                DdcErrorStatus.RESOURCE_ADMISSION_EXPIRED
        );
    }

    @Test
    void rejectsWrongContextAudienceScopeAndSourceBinding() {
        assertFailure(
                () -> verifier(new AccessTokenVerification.Valid<>(principal(
                        ServiceTokenContext.TENANT,
                        "tenant-1",
                        RESOURCE_URI,
                        Set.of(IdpJwtDdcRegistrationCredentialVerifier.REGISTRATION_SCOPE),
                        "permission", "idp", "prod")))
                        .verify("token", "permission", "idp", "prod", "idp-1"),
                DdcErrorStatus.RESOURCE_ADMISSION_INVALID
        );
        assertFailure(
                () -> verifier(new AccessTokenVerification.Valid<>(principal(
                        ServiceTokenContext.PLATFORM,
                        null,
                        URI.create("https://api.example/other"),
                        Set.of(IdpJwtDdcRegistrationCredentialVerifier.REGISTRATION_SCOPE),
                        "permission", "idp", "prod")))
                        .verify("token", "permission", "idp", "prod", "idp-1"),
                DdcErrorStatus.RESOURCE_ADMISSION_INVALID
        );
        assertFailure(
                () -> verifier(new AccessTokenVerification.Valid<>(principal(
                        ServiceTokenContext.PLATFORM,
                        null,
                        RESOURCE_URI,
                        Set.of("ddc:read"),
                        "permission", "idp", "prod")))
                        .verify("token", "permission", "idp", "prod", "idp-1"),
                DdcErrorStatus.RESOURCE_ADMISSION_INVALID
        );
        assertFailure(
                () -> verifier(new AccessTokenVerification.Valid<>(principal(
                        ServiceTokenContext.PLATFORM,
                        null,
                        RESOURCE_URI,
                        Set.of(IdpJwtDdcRegistrationCredentialVerifier.REGISTRATION_SCOPE),
                        "other", "idp", "prod")))
                        .verify("token", "permission", "idp", "prod", "idp-1"),
                DdcErrorStatus.RESOURCE_ADMISSION_BINDING_MISMATCH
        );
    }

    @Test
    void rejectsExpiredPrincipalAndOversizeInstanceBinding() {
        ServiceIdentityPrincipal expired = new ServiceIdentityPrincipal(
                "client-id", null, "client-id", "token-1", RESOURCE_URI, 7L,
                Set.of(IdpJwtDdcRegistrationCredentialVerifier.REGISTRATION_SCOPE),
                "permission", "idp", "prod", "credential-1",
                NOW.minusSeconds(60), NOW,
                "app-id", ServiceTokenContext.PLATFORM
        );
        assertFailure(
                () -> verifier(new AccessTokenVerification.Valid<>(expired))
                        .verify("token", "permission", "idp", "prod", "idp-1"),
                DdcErrorStatus.RESOURCE_ADMISSION_INVALID
        );

        assertFailure(
                () -> verifier(new AccessTokenVerification.Valid<>(principal(
                        ServiceTokenContext.PLATFORM,
                        null,
                        RESOURCE_URI,
                        Set.of(IdpJwtDdcRegistrationCredentialVerifier.REGISTRATION_SCOPE),
                        "permission", "idp", "prod")))
                        .verify("token", "permission", "idp", "prod", "x".repeat(129)),
                DdcErrorStatus.RESOURCE_ADMISSION_BINDING_MISMATCH
        );
    }

    private IdpJwtDdcRegistrationCredentialVerifier verifier(
            AccessTokenVerification<ServiceIdentityPrincipal> result) {
        ServiceAccessTokenVerifier serviceTokens = mock(ServiceAccessTokenVerifier.class);
        when(serviceTokens.verify(anyString())).thenReturn(result);
        return new IdpJwtDdcRegistrationCredentialVerifier(
                serviceTokens,
                "permission-idp-prod",
                RESOURCE_URI,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private ServiceIdentityPrincipal principal(
            ServiceTokenContext context,
            String tenantId,
            URI resourceUri,
            Set<String> scopes,
            String bizCode,
            String appCode,
            String environment) {
        return new ServiceIdentityPrincipal(
                "client-id",
                tenantId,
                "client-id",
                "token-1",
                resourceUri,
                7L,
                scopes,
                bizCode,
                appCode,
                environment,
                "credential-1",
                NOW.minusSeconds(5),
                NOW.plusSeconds(40),
                "app-id",
                context
        );
    }

    private void assertFailure(
            Runnable operation,
            DdcErrorStatus status) {
        assertThatThrownBy(operation::run)
                .isInstanceOf(DdcRegistrationAuthenticationException.class)
                .extracting(failure ->
                        ((DdcRegistrationAuthenticationException) failure).getStatus())
                .isEqualTo(status.getStatus());
    }
}
