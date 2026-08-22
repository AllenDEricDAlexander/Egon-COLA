package top.egon.cola.component.ddc.admin.security.registration;

import top.egon.cola.component.ddc.error.DdcErrorStatus;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.idp.contract.ServiceTokenContext;
import top.egon.cola.platform.idp.starter.security.AccessTokenVerification;
import top.egon.cola.platform.idp.starter.security.ServiceAccessTokenVerifier;

import java.net.URI;
import java.time.Clock;
import java.util.Objects;

/**
 * Verifies DDC registration credentials through the shared IdP SERVICE-token verifier.
 *
 * <p>The IdP verifier owns JWT algorithm, issuer, signature, client state and Resource
 * projection checks. This adapter adds the DDC-specific PLATFORM context, audience, scope and
 * physical request binding checks before a lease service can persist the identity.</p>
 */
public final class IdpJwtDdcRegistrationCredentialVerifier
        implements DdcRegistrationCredentialVerifier {

    /** Least-privilege scope required for register and heartbeat mutations. */
    public static final String REGISTRATION_SCOPE = "ddc:registration:write";

    private final ServiceAccessTokenVerifier serviceTokens;

    private final String resourceServerId;

    private final URI resourceUri;

    private final String requiredScope;

    private final Clock clock;

    /** Creates the production DDC registration verifier. */
    public IdpJwtDdcRegistrationCredentialVerifier(
            ServiceAccessTokenVerifier serviceTokens,
            String resourceServerId,
            URI resourceUri) {
        this(
                serviceTokens,
                resourceServerId,
                resourceUri,
                REGISTRATION_SCOPE,
                Clock.systemUTC()
        );
    }

    /** Creates a verifier with an injectable clock for deterministic tests. */
    public IdpJwtDdcRegistrationCredentialVerifier(
            ServiceAccessTokenVerifier serviceTokens,
            String resourceServerId,
            URI resourceUri,
            Clock clock) {
        this(
                serviceTokens,
                resourceServerId,
                resourceUri,
                REGISTRATION_SCOPE,
                clock
        );
    }

    /** Creates a verifier with an injectable scope and clock. */
    public IdpJwtDdcRegistrationCredentialVerifier(
            ServiceAccessTokenVerifier serviceTokens,
            String resourceServerId,
            URI resourceUri,
            String requiredScope,
            Clock clock) {
        this.serviceTokens = Objects.requireNonNull(
                serviceTokens,
                "serviceTokens"
        );
        this.resourceServerId = required(resourceServerId, "resourceServerId");
        this.resourceUri = validResource(resourceUri);
        this.requiredScope = required(requiredScope, "requiredScope");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public VerifiedDdcRegistrationIdentity verify(
            String registrationToken,
            String bizCode,
            String appCode,
            String env,
            String instanceId) {
        if (registrationToken == null || registrationToken.isBlank()) {
            throw failure(DdcErrorStatus.RESOURCE_ADMISSION_REQUIRED);
        }
        try {
            AccessTokenVerification<ServiceIdentityPrincipal> result =
                    serviceTokens.verify(registrationToken);
            if (result instanceof AccessTokenVerification.Expired<?>) {
                throw failure(DdcErrorStatus.RESOURCE_ADMISSION_EXPIRED);
            }
            if (result instanceof AccessTokenVerification.Invalid<?> invalid) {
                throw failure(
                        DdcErrorStatus.RESOURCE_ADMISSION_INVALID,
                        new IllegalStateException(invalid.reasonCode())
                );
            }
            ServiceIdentityPrincipal principal =
                    ((AccessTokenVerification.Valid<ServiceIdentityPrincipal>) result)
                            .principal();
            validateToken(principal);
            validateBinding(principal, bizCode, appCode, env, instanceId);
            return new VerifiedDdcRegistrationIdentity(
                    principal.appId(),
                    principal.clientId(),
                    resourceServerId,
                    principal.resourceUri().toString(),
                    principal.resourceVersion(),
                    principal.sourceBizCode(),
                    principal.sourceAppCode(),
                    principal.sourceEnvironment(),
                    required(instanceId, "instanceId"),
                    principal.credentialId(),
                    principal.tokenId(),
                    principal.issuedAt(),
                    principal.expiresAt(),
                    principal.scopes()
            );
        } catch (DdcRegistrationAuthenticationException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw failure(DdcErrorStatus.RESOURCE_ADMISSION_INVALID, failure);
        }
    }

    private void validateToken(ServiceIdentityPrincipal principal) {
        if (principal.scopeContext() != ServiceTokenContext.PLATFORM
                || principal.tenantId() != null
                || !resourceUri.equals(principal.resourceUri())
                || !principal.scopes().contains(requiredScope)
                || !principal.expiresAt().isAfter(clock.instant())
                || !principal.expiresAt().isAfter(principal.issuedAt())) {
            throw failure(DdcErrorStatus.RESOURCE_ADMISSION_INVALID);
        }
    }

    private void validateBinding(
            ServiceIdentityPrincipal principal,
            String bizCode,
            String appCode,
            String env,
            String instanceId) {
        if (!principal.sourceBizCode().equals(bizCode)
                || !principal.sourceAppCode().equals(appCode)
                || !principal.sourceEnvironment().equals(env)
                || required(instanceId, "instanceId").length() > 128) {
            throw failure(DdcErrorStatus.RESOURCE_ADMISSION_BINDING_MISMATCH);
        }
    }

    private DdcRegistrationAuthenticationException failure(
            DdcErrorStatus status) {
        return new DdcRegistrationAuthenticationException(status);
    }

    private DdcRegistrationAuthenticationException failure(
            DdcErrorStatus status,
            Throwable cause) {
        return new DdcRegistrationAuthenticationException(status, cause);
    }

    private static URI validResource(URI value) {
        Objects.requireNonNull(value, "resourceUri");
        if (!value.isAbsolute()
                || value.getFragment() != null
                || !value.equals(value.normalize())) {
            throw new IllegalArgumentException("resourceUri is invalid");
        }
        return value;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
