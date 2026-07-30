package top.egon.cola.platform.rbac3.contract.auth;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record Rbac3TokenClaims(
        String iss,
        List<String> aud,
        String sub,
        String tid,
        String sid,
        long av,
        long sv,
        long pv,
        String jti,
        Instant iat,
        Instant nbf,
        Instant exp,
        String kid
) {

    public Rbac3TokenClaims {
        iss = required(iss, "iss");
        aud = requiredValues(aud, "aud");
        sub = required(sub, "sub");
        tid = required(tid, "tid");
        sid = required(sid, "sid");
        nonNegative(av, "av");
        nonNegative(sv, "sv");
        nonNegative(pv, "pv");
        jti = required(jti, "jti");
        iat = Objects.requireNonNull(iat, "iat");
        nbf = Objects.requireNonNull(nbf, "nbf");
        exp = Objects.requireNonNull(exp, "exp");
        kid = required(kid, "kid");
        if (nbf.isBefore(iat) || !exp.isAfter(nbf)) {
            throw new IllegalArgumentException(
                    "token times must satisfy exp > nbf >= iat"
            );
        }
    }

    private static List<String> requiredValues(
            List<String> values,
            String fieldName) {
        List<String> copy = List.copyOf(Objects.requireNonNull(
                values,
                fieldName
        ));
        if (copy.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        copy.forEach(value -> required(value, fieldName));
        return copy;
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static void nonNegative(long value, String fieldName) {
        if (value < 0L) {
            throw new IllegalArgumentException(
                    fieldName + " must not be negative"
            );
        }
    }
}
