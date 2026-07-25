package top.egon.cola.component.ddc.admin.service;

import top.egon.cola.component.ddc.admin.common.DdcAdminException;

import java.nio.charset.StandardCharsets;

public final class DdcConfigValueGuard {

    public static final long DEFAULT_MAX_VALUE_BYTES = 1024L * 1024L;

    private final long maxValueBytes;

    public DdcConfigValueGuard(long maxValueBytes) {
        if (maxValueBytes <= 0) {
            throw new IllegalArgumentException("maxValueBytes must be positive");
        }
        this.maxValueBytes = maxValueBytes;
    }

    public void check(String value) {
        long bytes = value == null
                ? 0
                : value.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > maxValueBytes) {
            throw new DdcAdminException(
                    "config value exceeds the UTF-8 limit of "
                            + maxValueBytes
                            + " bytes"
            );
        }
    }
}
