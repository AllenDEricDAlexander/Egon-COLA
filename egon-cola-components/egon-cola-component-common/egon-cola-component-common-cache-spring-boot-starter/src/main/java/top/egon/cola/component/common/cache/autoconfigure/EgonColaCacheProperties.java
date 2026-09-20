package top.egon.cola.component.common.cache.autoconfigure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 两级缓存组件配置面，键树以主 Spec §16 为准；默认整体关闭（{@code enabled=false}），
 * 宿主未显式开启时零影响。
 */
@Data
@Validated
@ConfigurationProperties(prefix = "egon.cola.component.cache")
public class EgonColaCacheProperties {

    private boolean enabled = false;
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
        private Duration expire = Duration.ofMinutes(30);
        @NotNull
        private Duration nullExpire = Duration.ofSeconds(60);
        @DecimalMin("0.0")
        @DecimalMax("0.5")
        private double jitterRatio = 0.1;

        @AssertTrue(message = "TTL must be at least one millisecond")
        public boolean isDurationValid() {
            return expire != null && expire.toMillis() > 0
                    && nullExpire != null && nullExpire.toMillis() > 0;
        }
    }

    @Data
    public static class RegionTtl {

        private Duration expire;
        private Duration nullExpire;
        @DecimalMin("0.0")
        @DecimalMax("0.5")
        private Double jitterRatio;

        @AssertTrue(message = "region TTL must be at least one millisecond")
        public boolean isDurationValid() {
            return (expire == null || expire.toMillis() > 0)
                    && (nullExpire == null || nullExpire.toMillis() > 0);
        }
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
