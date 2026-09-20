package top.egon.cola.component.common.cache.autoconfigure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 两级缓存组件配置面，键树以主 Spec §16 为准。组件是必需的（{@code enabled=true} 缺省），
 * 且 {@code ignoreUnknownFields = false}：旧共享 TTL 键（{@code ttl.expire}、
 * {@code ttl.jitter-ratio}）与任何未知键在绑定阶段即被拒绝，不会静默降级。
 */
@Data
@Validated
@ConfigurationProperties(prefix = EgonColaCacheProperties.PREFIX, ignoreUnknownFields = false)
public class EgonColaCacheProperties {

    /** 配置键树前缀，条件装配与跨组件裁决共用同一字面串。 */
    public static final String PREFIX = "egon.cola.component.cache";

    private boolean enabled = true;
    private String nodeId = "";
    private String keyPrefix = "egon:cola:cache";
    @Valid
    private Redis redis = new Redis();
    private String tenantMdcKey = "tenantId";
    @NotNull
    private Duration secondEvictDelay = Duration.ofSeconds(5);
    @Valid
    private L1 l1 = new L1();
    @Valid
    private Ttl ttl = new Ttl();
    /**
     * 按区域覆写 TTL，未配置字段继承全局 ttl。
     */
    @Valid
    private Map<String, RegionTtl> regions = new LinkedHashMap<>();
    @Valid
    private Batch batch = new Batch();
    @Valid
    private Lock lock = new Lock();

    @Data
    public static class Redis {

        private String topic = "egon:cola:cache:event";
    }

    @Data
    public static class L1 {

        @Positive
        private int maxSize = 10000;
    }

    @Data
    public static class Ttl {

        @NotNull
        private Duration l1Expire = Duration.ofMinutes(5);
        @NotNull
        private Duration l1Jitter = Duration.ofMinutes(2);
        @NotNull
        private Duration l2Expire = Duration.ofHours(1);
        @NotNull
        private Duration l2Jitter = Duration.ofMinutes(20);
        @NotNull
        private Duration nullExpire = Duration.ofSeconds(60);

        @AssertTrue(message = "TTLs must be positive and their jitter sum must not overflow")
        public boolean isDurationValid() {
            return positive(nullExpire) && bounded(l1Expire, l1Jitter) && bounded(l2Expire, l2Jitter);
        }
    }

    @Data
    public static class RegionTtl {

        private Duration l1Expire;
        private Duration l1Jitter;
        private Duration l2Expire;
        private Duration l2Jitter;
        private Duration nullExpire;

        @AssertTrue(message = "region TTLs must be positive and their jitter sum must not overflow")
        public boolean isDurationValid() {
            return (nullExpire == null || positive(nullExpire))
                    && bounded(l1Expire, l1Jitter) && bounded(l2Expire, l2Jitter);
        }
    }

    private static boolean positive(Duration duration) {
        return duration != null && !duration.isZero() && !duration.isNegative();
    }

    /**
     * 缺字段（区域继承全局）单独放行，存在则基准必须为正、抖动非负且两者之差仍可表示为毫秒。
     */
    static boolean bounded(Duration base, Duration jitter) {
        if (base == null) {
            return true;
        }
        if (!positive(base) || (jitter != null && jitter.isNegative())) {
            return false;
        }
        try {
            jittered(base, jitter).toMillis();
            return true;
        } catch (ArithmeticException ex) {
            return false;
        }
    }

    private static Duration jittered(Duration base, Duration jitter) {
        return jitter == null ? base : base.plus(jitter);
    }

    @Data
    public static class Batch {

        @Positive
        private int maxKeys = 1000;
    }

    @Data
    public static class Lock {

        @NotNull
        private Duration waitTime = Duration.ofMillis(500);
        @NotNull
        private Duration leaseTime = Duration.ofSeconds(10);
    }
}
