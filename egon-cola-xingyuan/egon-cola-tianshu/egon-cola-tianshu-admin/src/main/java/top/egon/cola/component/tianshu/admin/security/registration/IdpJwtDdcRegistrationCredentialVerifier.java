package top.egon.cola.component.tianshu.admin.security.registration;

import top.egon.cola.component.tianshu.error.DdcErrorStatus;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext;
import top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification;
import top.egon.cola.platform.tianquan.shoubing.starter.security.ServiceAccessTokenVerifier;

import java.net.URI;
import java.time.Clock;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Verifies Tianshu registration credentials through the shared Tianquan-Shoubing SERVICE-token verifier.
 *
 * <p>The Tianquan-Shoubing verifier owns JWT algorithm, issuer, signature, client state and Resource
 * projection checks. This adapter adds the Tianshu-specific PLATFORM context, audience, scope and
 * physical request binding checks before a lease service can persist the identity.</p>
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class IdpJwtDdcRegistrationCredentialVerifier
        implements DdcRegistrationCredentialVerifier {

    /** Least-privilege scope required for register and heartbeat mutations. */
    public static final String REGISTRATION_SCOPE = "tianshu:registration:write";

    @Qualifier("serviceAccessTokenVerifier") private final ServiceAccessTokenVerifier serviceTokens;
    private final DdcRegistrationPolicyBO policy;
    @Qualifier("idpClock") private final Clock clock;
    private final java.util.concurrent.atomic.AtomicReference<String> lastUnavailableReason =
            new java.util.concurrent.atomic.AtomicReference<>();

    /** Creates the production Tianshu registration verifier. */
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
        this(Objects.requireNonNull(serviceTokens, "serviceTokens"), new DdcRegistrationPolicyBO(
                required(resourceServerId, "resourceServerId"), validResource(resourceUri), required(requiredScope, "requiredScope")), Objects.requireNonNull(clock, "clock"));
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
            if (result instanceof AccessTokenVerification.Unavailable<?> unavailable) {
                if (!unavailable.reasonCode().equals(lastUnavailableReason.getAndSet(unavailable.reasonCode()))) {
                    log.warn("Tianshu registration credential dependency unavailable; bizCode={} appCode={} env={} reasonCode={}",
                            bizCode, appCode, env, unavailable.reasonCode());
                }
                throw failure(DdcErrorStatus.RESOURCE_ADMISSION_UNAVAILABLE);
            }
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
            lastUnavailableReason.set(null);
            return new VerifiedDdcRegistrationIdentity(
                    principal.appId(),
                    principal.clientId(),
                    policy.resourceServerId(),
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
                || !policy.resourceUri().equals(principal.resourceUri())
                || !principal.scopes().contains(policy.requiredScope())
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

    /** Immutable local construction settings keep all former public overloads unchanged. */
    private record DdcRegistrationPolicyBO(String resourceServerId, URI resourceUri, String requiredScope) { }
}
