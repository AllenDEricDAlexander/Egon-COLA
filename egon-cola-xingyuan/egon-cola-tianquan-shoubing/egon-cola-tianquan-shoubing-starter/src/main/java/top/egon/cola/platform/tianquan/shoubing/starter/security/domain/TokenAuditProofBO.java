package top.egon.cola.platform.tianquan.shoubing.starter.security.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.net.URI;

/** Immutable, secret-free observation metadata; it is never an authorization credential. */
public record TokenAuditProofBO(@NotNull URI issuer, @NotNull URI resource,
        @Pattern(regexp = "[0-9a-f]{64}") String subjectRef,
        @Pattern(regexp = "[0-9a-f]{64}") String clientRef,
        @Pattern(regexp = "[0-9a-f]{64}") String tokenRef) {
    public TokenAuditProofBO {
        if (!https(issuer) || !https(resource) || !reference(subjectRef) || !reference(clientRef) || !reference(tokenRef)) {
            throw new IllegalArgumentException("TOKEN_AUDIT_PROOF_INVALID");
        }
    }

    private static boolean https(URI value) {
        return value != null && value.isAbsolute() && "https".equalsIgnoreCase(value.getScheme())
                && value.getHost() != null && value.getUserInfo() == null && value.getQuery() == null && value.getFragment() == null;
    }

    private static boolean reference(String value) { return value == null || value.matches("[0-9a-f]{64}"); }
}
