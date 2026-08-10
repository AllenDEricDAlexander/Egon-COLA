package top.egon.cola.platform.idp.admin.support.ddc;

import top.egon.cola.component.ddc.annotation.DdcValue;

public final class IdpDdcValueDeclarations {

    @DdcValue(
            value = "${idp.token.access-ttl:900}",
            refreshable = false
    )
    private Long accessTokenTtlSeconds = 900L;

    @DdcValue(
            value = "${idp.token.refresh-ttl:604800}",
            refreshable = false
    )
    private Long refreshTokenTtlSeconds = 604_800L;

    @DdcValue(
            value = "${idp.authorization-code.ttl:60}",
            refreshable = false
    )
    private Long authorizationCodeTtlSeconds = 60L;

    @DdcValue(
            value = "${idp.login.max-failures:5}",
            refreshable = false
    )
    private Integer maximumLoginFailures = 5;

    @DdcValue(
            value = "${idp.login.lock-duration:900}",
            refreshable = false
    )
    private Long loginLockDurationSeconds = 900L;

    @DdcValue(
            value = "${idp.password.max-concurrency:8}",
            refreshable = false
    )
    private Integer passwordMaximumConcurrency = 8;
}
