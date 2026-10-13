package top.egon.cola.archetype.source.service.infrastructure.client.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Exact target scope for the native organization references. */
@Validated
@ConfigurationProperties(prefix = "app.integrations.organization")
public record NativeOrganizationRpcProperties(
        @NotBlank String bizCode,
        @NotBlank String appCode,
        @NotBlank String group,
        @NotBlank String version,
        @Positive long timeoutMs,
        @NotBlank @Size(max = 108) @Pattern(regexp = "[A-Za-z0-9._-]+") String applicationNamespace) {
}
