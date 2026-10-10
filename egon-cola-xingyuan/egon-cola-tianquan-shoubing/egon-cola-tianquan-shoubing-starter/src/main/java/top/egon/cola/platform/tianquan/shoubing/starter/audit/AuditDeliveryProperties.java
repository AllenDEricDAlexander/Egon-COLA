package top.egon.cola.platform.tianquan.shoubing.starter.audit;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Bounded receipt retention and local recovery; the original audit properties remain unchanged. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder @Accessors(chain = true)
@Validated
@ConfigurationProperties("egon.cola.platform.tianquan.shoubing.audit.delivery")
public class AuditDeliveryProperties {
    @NotNull @Builder.Default private Duration receiptTtl = Duration.ofHours(1);
    @NotNull @Builder.Default private Duration confirmationTimeout = Duration.ofSeconds(30);
    @Min(1) @Max(1000) @Builder.Default private int batchSize = 100;
    @AssertTrue public boolean isTimingValid() {
        return receiptTtl != null && confirmationTimeout != null
                && receiptTtl.compareTo(Duration.ofMinutes(1)) >= 0 && receiptTtl.compareTo(Duration.ofHours(24)) <= 0
                && confirmationTimeout.compareTo(Duration.ofSeconds(5)) >= 0 && confirmationTimeout.compareTo(Duration.ofMinutes(5)) <= 0;
    }
}
