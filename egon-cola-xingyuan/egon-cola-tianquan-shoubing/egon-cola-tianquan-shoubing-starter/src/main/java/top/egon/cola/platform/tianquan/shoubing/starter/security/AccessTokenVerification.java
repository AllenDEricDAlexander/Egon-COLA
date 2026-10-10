package top.egon.cola.platform.tianquan.shoubing.starter.security;

import top.egon.cola.platform.tianquan.shoubing.contract.IdpPrincipal;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Explicit outcome of access-token verification.
 */
public sealed interface AccessTokenVerification<T extends IdpPrincipal>
        permits AccessTokenVerification.Valid,
        AccessTokenVerification.Expired,
        AccessTokenVerification.Invalid,
        AccessTokenVerification.Unavailable {

    record Valid<T extends IdpPrincipal>(T principal, Set<String> scopes,
            @com.fasterxml.jackson.annotation.JsonIgnore @jakarta.validation.Valid
            top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO proof)
            implements AccessTokenVerification<T> {

        public Valid {
            principal = Objects.requireNonNull(principal, "principal");
            TreeSet<String> normalized = new TreeSet<>();
            for (String scope : Objects.requireNonNull(scopes, "scopes")) {
                if (scope == null || !scope.matches("[A-Za-z0-9:._-]{1,128}")
                        || !normalized.add(scope)) {
                    throw new IllegalArgumentException("scopes are invalid");
                }
            }
            if (normalized.size() > 64) {
                throw new IllegalArgumentException("scopes exceed the allowed count");
            }
            scopes = Collections.unmodifiableSet(normalized);
        }

        public Valid(T principal, Set<String> scopes) { this(principal, scopes, null); }

        public Valid(T principal) {
            this(principal, Set.of(), null);
        }
    }

    record Expired<T extends IdpPrincipal>()
            implements AccessTokenVerification<T> {
    }

    record Invalid<T extends IdpPrincipal>(String reasonCode)
            implements AccessTokenVerification<T> {
    }

    record Unavailable<T extends IdpPrincipal>(String reasonCode)
            implements AccessTokenVerification<T> {

        public Unavailable {
            if (reasonCode == null || !reasonCode.matches("[A-Z0-9_]{1,96}")) {
                throw new IllegalArgumentException("reasonCode is invalid");
            }
        }
    }
}
