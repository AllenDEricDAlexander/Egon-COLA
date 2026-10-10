package top.egon.cola.platform.tianquan.shoubing.core.observability;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Duration;
import java.util.Set;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** Bounded technical sample; identities and arbitrary metric tags never enter this contract. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class SecurityMetricObservationBO {
    @NotBlank
    @Pattern(regexp = "jwt\\.verify\\.duration|jwks\\.cache\\.access|token\\.cache\\.access|token\\.acquire\\.duration|oauth\\.authentication|rbac\\.authorization|oauth\\.audit\\.delivery|receipt\\.age|spool\\.bytes")
    private String signal;
    @NotBlank
    @Pattern(regexp = "SUCCESS|FAILURE|VALID|INVALID|EXPIRED|UNAVAILABLE|ALLOW|DENY|INDETERMINATE|HIT|MISS|STAGED|SENT|CONFIRMED|FAILED|DEAD|CURRENT")
    private String result;
    private Duration duration;
    @PositiveOrZero
    private long value;

    private static final Map<String, Set<String>> RESULTS = Map.of(
            "jwt.verify.duration", Set.of("SUCCESS", "FAILURE"),
            "jwks.cache.access", Set.of("HIT", "MISS", "EXPIRED"),
            "token.cache.access", Set.of("HIT", "MISS", "EXPIRED"),
            "token.acquire.duration", Set.of("SUCCESS", "FAILURE"),
            "oauth.authentication", Set.of("VALID", "INVALID", "EXPIRED", "UNAVAILABLE"),
            "rbac.authorization", Set.of("ALLOW", "DENY", "INDETERMINATE", "FAILURE", "UNAVAILABLE"),
            "oauth.audit.delivery", Set.of("STAGED", "SENT", "CONFIRMED", "FAILED", "DEAD", "EXPIRED"),
            "receipt.age", Set.of("CURRENT", "CONFIRMED"),
            "spool.bytes", Set.of("CURRENT"));

    @AssertTrue
    @java.beans.Transient
    public boolean isShapeValid() {
        if (signal == null || result == null || !RESULTS.getOrDefault(signal, Set.of()).contains(result)) { return false; }
        boolean timer = Set.of("jwt.verify.duration", "token.acquire.duration", "receipt.age").contains(signal);
        boolean gauge = "spool.bytes".equals(signal);
        return timer ? duration != null && !duration.isNegative() && value == 0
                : duration == null && (gauge || value == 0);
    }
}
