package top.egon.cola.component.dtp.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.Accessors;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * @ClassName: DynamicThreadPoolAutoProperties
 * @description: 动态线程池配置
 * @author: 有罗敷的马同学
 * @datetime: 2025Year-04Month-14Day-下午8:21
 * @Version: 1.0
 */
@ConfigurationProperties(prefix = "egon.cola.component.dtp", ignoreInvalidFields = true)
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Validated
public class DynamicThreadPoolAutoProperties {

    private boolean enabled = true;

    private String appName;

    private String instanceId;

    private Registry registry = new Registry();

    private Report report = new Report();

    @Valid
    @NotNull
    private Observation observation = new Observation();

    @Valid
    @NotNull
    private ContextPropagation contextPropagation = new ContextPropagation();

    @Valid
    @NotNull
    private ExceptionHandling exceptionHandling = new ExceptionHandling();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class ContextPropagation {
        @Builder.Default
        private boolean customThreadLocalsEnabled = true;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class ExceptionHandling {
        @Builder.Default
        private boolean enabled = true;
        @NotNull
        @Builder.Default
        private Duration logInterval = Duration.ofSeconds(1);
        @Builder.Default
        private boolean uncaughtEnabled = true;
        @Builder.Default
        private boolean asyncVoidEnabled = true;

        @JsonIgnore
        @AssertTrue(message = "exception log interval must be positive and fit nanoseconds")
        public boolean isDurationValid() {
            return positiveDuration(logInterval);
        }
    }

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    public static class Registry {

        private String type = "redis";

        private Redis redis = new Redis();

    }

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    public static class Redis {

        private String host = "127.0.0.1";

        private int port = 6379;

        @ToString.Exclude
        private String password;

        private int database = 0;

        private int poolSize = 64;

        private int minIdleSize = 10;

        private int idleTimeout = 10000;

        private int connectTimeout = 10000;

        private int retryAttempts = 3;

        private int retryInterval = 1000;

        private int pingInterval = 0;

        private boolean keepAlive = true;

    }

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    public static class Report {

        private boolean enabled = true;

        private Duration interval = Duration.ofSeconds(20);

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class Observation {

        @Builder.Default
        private boolean enabled = true;

        @NotNull
        @Builder.Default
        private Duration slowTaskThreshold = Duration.ofSeconds(30);

        @NotNull
        @Builder.Default
        private Duration queueWaitThreshold = Duration.ofSeconds(5);

        @Valid
        @NotNull
        @Builder.Default
        private Alarm alarm = new Alarm();

        @JsonIgnore
        @AssertTrue(message = "observation thresholds must be positive and fit nanoseconds")
        public boolean isDurationValid() {
            return positiveDuration(slowTaskThreshold) && positiveDuration(queueWaitThreshold);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class Alarm {

        private boolean enabled;

        @DecimalMin(value = "0", inclusive = false)
        @DecimalMax("1")
        @Builder.Default
        private double queueUsageThreshold = 0.8;

        @Min(1)
        @Builder.Default
        private int consecutiveSamples = 3;

        @NotNull
        @Builder.Default
        private Duration cooldown = Duration.ofSeconds(60);

        @JsonIgnore
        @AssertTrue(message = "alarm cooldown must be positive and fit nanoseconds")
        public boolean isDurationValid() {
            return positiveDuration(cooldown);
        }
    }

    private static boolean positiveDuration(Duration value) {
        return value != null && !value.isZero() && !value.isNegative()
                && value.compareTo(Duration.ofNanos(Long.MAX_VALUE)) <= 0;
    }

}
