package top.egon.cola.platform.idp.admin.integration.ddc;

import top.egon.cola.component.ddc.annotation.DdcValue;

public final class IdpDdcValueDeclarations {

    @DdcValue(
            value = "idp.token.access-ttl:900",
            key = AtomicIdpRuntimePolicy.ACCESS_TOKEN_TTL_KEY,
            defaultValue = "900",
            type = Long.class,
            required = true,
            refreshable = false
    )
    private Long accessTokenTtlSeconds = 900L;

    @DdcValue(
            value = "idp.token.refresh-ttl:604800",
            key = AtomicIdpRuntimePolicy.REFRESH_TOKEN_TTL_KEY,
            defaultValue = "604800",
            type = Long.class,
            required = true,
            refreshable = false
    )
    private Long refreshTokenTtlSeconds = 604_800L;

    @DdcValue(
            value = "idp.authorization-code.ttl:60",
            key = AtomicIdpRuntimePolicy.AUTHORIZATION_CODE_TTL_KEY,
            defaultValue = "60",
            type = Long.class,
            required = true,
            refreshable = false
    )
    private Long authorizationCodeTtlSeconds = 60L;

    @DdcValue(
            value = "idp.login.max-failures:5",
            key = AtomicIdpRuntimePolicy.MAXIMUM_LOGIN_FAILURES_KEY,
            defaultValue = "5",
            type = Integer.class,
            required = true,
            refreshable = false
    )
    private Integer maximumLoginFailures = 5;

    @DdcValue(
            value = "idp.login.lock-duration:900",
            key = AtomicIdpRuntimePolicy.LOGIN_LOCK_DURATION_KEY,
            defaultValue = "900",
            type = Long.class,
            required = true,
            refreshable = false
    )
    private Long loginLockDurationSeconds = 900L;

    @DdcValue(
            value = "idp.password.max-concurrency:8",
            key = AtomicIdpRuntimePolicy.PASSWORD_MAXIMUM_CONCURRENCY_KEY,
            defaultValue = "8",
            type = Integer.class,
            required = true,
            refreshable = false
    )
    private Integer passwordMaximumConcurrency = 8;
}
