package top.egon.cola.platform.rbac3.admin.integration.ddc;

import top.egon.cola.component.ddc.annotation.DdcValue;

/**
 * Declares the RBAC3 configuration catalog reported to DDC.
 */
public final class Rbac3DdcValueDeclarations {

    @DdcValue(
            value = "rbac3.access-token-ttl-seconds:900",
            key = AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY,
            defaultValue = "900",
            type = Long.class,
            required = true,
            refreshable = false)
    private Long accessTokenTtlSeconds = 900L;

    @DdcValue(
            value = "rbac3.refresh-token-ttl-seconds:604800",
            key = AtomicRbac3RuntimePolicy.REFRESH_TOKEN_TTL_KEY,
            defaultValue = "604800",
            type = Long.class,
            required = true,
            refreshable = false)
    private Long refreshTokenTtlSeconds = 604_800L;

    @DdcValue(
            value = "rbac3.session-idle-timeout-seconds:1800",
            key = AtomicRbac3RuntimePolicy.SESSION_IDLE_TIMEOUT_KEY,
            defaultValue = "1800",
            type = Long.class,
            required = true,
            refreshable = false)
    private Long sessionIdleTimeoutSeconds = 1_800L;

    @DdcValue(
            value = "rbac3.session-absolute-timeout-seconds:43200",
            key = AtomicRbac3RuntimePolicy.SESSION_ABSOLUTE_TIMEOUT_KEY,
            defaultValue = "43200",
            type = Long.class,
            required = true,
            refreshable = false)
    private Long sessionAbsoluteTimeoutSeconds = 43_200L;

    @DdcValue(
            value = "rbac3.maximum-active-roots:16",
            key = AtomicRbac3RuntimePolicy.MAXIMUM_ACTIVE_ROOTS_KEY,
            defaultValue = "16",
            type = Integer.class,
            required = true,
            refreshable = false)
    private Integer maximumActiveRoots = 16;
}
