package top.egon.cola.archetype.source.web.infrastructure.client.evaluation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Exact target scope for the native evaluation references. */
@Validated
@ConfigurationProperties(prefix = "organization.integrations.evaluation")
public record NativeEvaluationRpcProperties(
        @NotBlank String bizCode,
        @NotBlank String appCode,
        @NotBlank String courseGroup,
        @NotBlank String examGroup,
        @NotBlank String scoreGroup,
        @NotBlank String version,
        @Positive long timeoutMs) {
}
