package top.egon.cola.platform.tianquan.shoubing.core.observability;

import jakarta.validation.Valid;

/** Host-owned metrics adapter; the SDK has no dependency on a registry implementation. */
@FunctionalInterface
public interface IdpSecurityMetricsRecorder {
    IdpSecurityMetricsRecorder NOOP = observation -> { };

    void record(@Valid SecurityMetricObservationBO observation);
}
