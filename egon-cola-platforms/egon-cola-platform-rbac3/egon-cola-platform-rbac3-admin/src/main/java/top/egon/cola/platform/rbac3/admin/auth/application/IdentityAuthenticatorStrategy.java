package top.egon.cola.platform.rbac3.admin.auth.application;

import top.egon.cola.platform.rbac3.contract.auth.LoginRequest;

import java.time.Instant;

/**
 * Verifies an identity credential without deriving any authorization data.
 */
public interface IdentityAuthenticatorStrategy {

    AuthenticatedIdentity authenticate(LoginRequest request, Instant now);

    record AuthenticatedIdentity(
            String tenantId,
            String userId,
            String authenticationMethod,
            int authenticationStrength
    ) {
    }
}
