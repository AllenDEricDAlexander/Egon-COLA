package top.egon.cola.platform.rbac3.admin.runtime.domain.vo;

import java.util.Map;
import java.util.Objects;

/**
 * Runtime RBAC policy; token and authentication lifecycle are IdP concerns.
 */
public record Rbac3RuntimePolicySnapshotVO(
        int maximumActiveRoots,
        Map<String, Long> configVersions) {

    public Rbac3RuntimePolicySnapshotVO {
        if (maximumActiveRoots < 1 || maximumActiveRoots > 32) {
            throw new IllegalArgumentException("MAXIMUM_ACTIVE_ROOTS_OUT_OF_RANGE");
        }
        configVersions = Map.copyOf(Objects.requireNonNull(configVersions, "configVersions"));
        configVersions.forEach((key, version) -> {
            if (key == null || key.isBlank() || version == null || version < 0) {
                throw new IllegalArgumentException("INVALID_CONFIG_VERSION");
            }
        });
    }
}
