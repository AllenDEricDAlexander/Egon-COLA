package top.egon.cola.component.tianshu.model.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import top.egon.cola.component.tianshu.model.ai.validation.ConsistentAiVersionSelector;

/** Read selector for the configured business, environment and application scope. */
@ConsistentAiVersionSelector
public record DdcAiResourceQuery(
        @NotBlank @Size(max = 128) @Pattern(regexp = "^[^\\p{Cntrl}]+$") String namespaceCode,
        @NotBlank @Size(max = 128) @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$") String resourceKey,
        @Positive Long versionNo,
        @NotNull @Positive Long resourceId) {

    public DdcAiResourceQuery {
        namespaceCode = namespaceCode == null ? null : namespaceCode.trim();
        resourceKey = resourceKey == null ? null : resourceKey.trim();
    }
}
