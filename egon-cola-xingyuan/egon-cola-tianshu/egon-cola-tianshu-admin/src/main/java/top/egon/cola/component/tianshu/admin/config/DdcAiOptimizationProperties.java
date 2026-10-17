package top.egon.cola.component.tianshu.admin.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 天枢 AI 使用反馈、自动评分与自动优化的 Admin 配置。
 * / Admin configuration for Tianshu AI usage feedback, automatic judging and automatic optimization.
 */
@ConfigurationProperties(prefix = DdcAiOptimizationProperties.PREFIX)
@Validated
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiOptimizationProperties {

    public static final String PREFIX = "egon.cola.component.tianshu.admin.ai";

    @NotNull
    @Valid
    @Builder.Default
    private Optimization optimization = new Optimization();

    @NotNull
    @Valid
    @Builder.Default
    private Judge judge = new Judge();

    @NotNull
    @Valid
    @Builder.Default
    private Model model = new Model();

    @NotNull
    @Valid
    @Builder.Default
    private Feedback feedback = new Feedback();

    /** 优化闭环开关与调度参数。 / Optimization loop switch and scheduling parameters. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class Optimization {

        private boolean enabled;

        @NotNull
        @Builder.Default
        private Duration scanInterval = Duration.ofSeconds(60);

        @NotNull
        @Builder.Default
        private Duration runLease = Duration.ofMinutes(10);

        @Min(1)
        @Max(16)
        @Builder.Default
        private int maxConcurrentRuns = 2;

        @Min(1)
        @Max(200)
        @Builder.Default
        private int sampleLimit = 20;
    }

    /** 自动评分作业参数。 / Automatic judge job parameters. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class Judge {

        @NotNull
        @Builder.Default
        private Duration scanInterval = Duration.ofSeconds(30);

        @Min(1)
        @Max(500)
        @Builder.Default
        private int batchSize = 50;

        @Min(1)
        @Max(10)
        @Builder.Default
        private int maxAttempts = 3;

        @Min(1024)
        @Max(262144)
        @Builder.Default
        private int maxContextBytes = 16384;

        @NotNull
        @Builder.Default
        private Duration lease = Duration.ofMinutes(5);
    }

    /** 玉衡 LLM 网关连接与认证参数。 / Yuheng LLM gateway connection and authentication parameters. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class Model {

        private String baseUrl;

        private String resourceUri;

        private String registrationId;

        private String appId;

        @NotNull
        @Builder.Default
        private List<String> scopes = new ArrayList<>();

        @NotNull
        @Builder.Default
        private Duration connectTimeout = Duration.ofSeconds(3);

        @NotNull
        @Builder.Default
        private Duration readTimeout = Duration.ofSeconds(120);

        @Min(1)
        @Max(32768)
        @Builder.Default
        private int maxOutputTokens = 8192;

        @DecimalMin("0")
        @DecimalMax("2")
        @Builder.Default
        private double temperature = 0.2d;
    }

    /** 使用上报的接收与保留参数。 / Usage-report intake and retention parameters. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class Feedback {

        @NotNull
        @Builder.Default
        private Duration retention = Duration.ofDays(30);

        @NotNull
        @Builder.Default
        private Duration retentionScanInterval = Duration.ofHours(1);

        @Min(1)
        @Max(100)
        @Builder.Default
        private int maxBatch = 100;

        @NotNull
        @Builder.Default
        private Duration maxClockSkew = Duration.ofMinutes(5);

        @Min(256)
        @Max(8192)
        @Builder.Default
        private int maxInputExcerptBytes = 8192;

        @Min(256)
        @Max(32768)
        @Builder.Default
        private int maxOutputExcerptBytes = 32768;
    }
}
