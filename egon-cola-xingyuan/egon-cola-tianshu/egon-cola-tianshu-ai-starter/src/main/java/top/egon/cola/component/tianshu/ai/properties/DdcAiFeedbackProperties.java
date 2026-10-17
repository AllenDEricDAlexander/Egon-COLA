package top.egon.cola.component.tianshu.ai.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Duration;

/** 使用上报队列与刷写配置。 / Usage-report queue and flush configuration. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiFeedbackProperties {

    private boolean enabled;

    @Min(1)
    @Max(1_000_000)
    @Builder.Default
    private int queueCapacity = 10_000;

    @Min(1)
    @Max(100)
    @Builder.Default
    private int batchSize = 100;

    @NotNull
    @Builder.Default
    private Duration flushInterval = Duration.ofSeconds(2);

    @Min(1)
    @Max(10)
    @Builder.Default
    private int maxAttempts = 3;
}
