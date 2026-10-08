package top.egon.cola.platform.tianquan.shoubing.admin.token.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.validation.annotation.Validated;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@Validated
@Qualifier("oauthKeyRotationProperties")
@ConfigurationProperties(prefix = "egon.tianquan-shoubing.oauth.key-rotation")
public class OAuthKeyRotationProperties {

    @Builder.Default
    @NotNull
    private Duration period = Duration.ofDays(90);

    @Builder.Default
    @NotNull
    private Duration prepublish = Duration.ofHours(24);

    @Builder.Default
    @NotBlank
    private String schedule = "0 0 2 * * *";

    public void validate() {
        if (period == null || period.compareTo(Duration.ofDays(90)) < 0
                || prepublish == null || prepublish.compareTo(Duration.ofHours(24)) < 0
                || prepublish.compareTo(period) >= 0) {
            throw new IllegalStateException("OAUTH_KEY_ROTATION_WINDOW_INVALID");
        }
        try {
            CronExpression.parse(schedule);
        } catch (IllegalArgumentException invalidCron) {
            throw new IllegalStateException("OAUTH_KEY_ROTATION_SCHEDULE_INVALID", invalidCron);
        }
    }
}
