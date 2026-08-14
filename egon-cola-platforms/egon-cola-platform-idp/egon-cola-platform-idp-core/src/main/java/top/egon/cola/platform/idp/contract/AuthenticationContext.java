package top.egon.cola.platform.idp.contract;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * USER token 中经过签名认证上下文。
 *
 * <p>Signed authentication context carried by a USER token.</p>
 *
 * @param acr 认证强度；authentication context class reference
 * @param authTime 原始认证时间；time of the authentication event
 */
public record AuthenticationContext(String acr, Instant authTime) {

    private static final Map<String, Integer> STRENGTHS = Map.of(
            "PASSWORD", 0,
            "MFA", 1,
            "STRONG", 2
    );

    /** Creates the default password-authentication context. */
    public static AuthenticationContext password() {
        return new AuthenticationContext("PASSWORD", Instant.now());
    }

    /** Creates a context with an explicit authentication time. */
    public static AuthenticationContext of(String acr, Instant authTime) {
        return new AuthenticationContext(acr, authTime);
    }

    public AuthenticationContext {
        acr = required(acr, "acr");
        if (!STRENGTHS.containsKey(acr)) {
            throw new IllegalArgumentException(
                    "unsupported authentication strength"
            );
        }
        authTime = Objects.requireNonNull(authTime, "authTime");
    }

    /**
     * Returns whether this context satisfies the required strength and age.
     *
     * @param requiredAcr minimum required strength
     * @param maxAge maximum allowed age
     * @param now current instant
     * @return true when the context is sufficient
     */
    public boolean satisfies(String requiredAcr, Duration maxAge, Instant now) {
        requiredAcr = required(requiredAcr, "requiredAcr");
        Objects.requireNonNull(maxAge, "maxAge");
        now = Objects.requireNonNull(now, "now");
        if (maxAge.isNegative() || maxAge.isZero()
                || !STRENGTHS.containsKey(requiredAcr)
                || !authTime.plus(maxAge).isAfter(now)) {
            return false;
        }
        return STRENGTHS.get(acr) >= STRENGTHS.get(requiredAcr);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
