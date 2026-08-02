package top.egon.cola.platform.rbac3.starter.client;

import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;

/** Retrieves one current-system authorization snapshot using service identity. */
@FunctionalInterface
public interface Rbac3AuthorizationClient {

    SystemAuthorizationSnapshot fetch(
            String systemCode,
            IdentityPrincipal principal) throws InterruptedException;

    final class AuthorizationUnavailableException extends RuntimeException {

        public AuthorizationUnavailableException(String reasonCode) {
            super(reasonCode);
        }

        public AuthorizationUnavailableException(String reasonCode, Throwable cause) {
            super(reasonCode, cause);
        }
    }

    final class AuthorizationDeniedException extends RuntimeException {

        public AuthorizationDeniedException(String reasonCode) {
            super(reasonCode);
        }
    }
}
