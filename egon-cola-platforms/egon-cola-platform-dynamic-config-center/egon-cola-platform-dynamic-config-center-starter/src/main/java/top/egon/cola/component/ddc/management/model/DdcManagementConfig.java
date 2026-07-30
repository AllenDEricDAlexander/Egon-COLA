package top.egon.cola.component.ddc.management.model;

import java.time.Instant;

public record DdcManagementConfig(
        String appCode,
        String env,
        String namespace,
        String configKey,
        String configValue,
        String valueType,
        Long version,
        boolean enabled,
        boolean deleted,
        Instant updatedAt
) {
}
