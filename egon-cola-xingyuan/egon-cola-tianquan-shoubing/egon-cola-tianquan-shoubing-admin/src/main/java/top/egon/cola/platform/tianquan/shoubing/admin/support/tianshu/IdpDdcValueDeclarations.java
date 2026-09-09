package top.egon.cola.platform.tianquan.shoubing.admin.support.tianshu;

import top.egon.cola.component.tianshu.annotation.DdcValue;

public final class IdpDdcValueDeclarations {

    @DdcValue(
            value = "${tianquan-shoubing.token.access-ttl:900}",
            refreshable = false
    )
    private Long accessTokenTtlSeconds = 900L;

    @DdcValue(
            value = "${tianquan-shoubing.token.refresh-ttl:604800}",
            refreshable = false
    )
    private Long refreshTokenTtlSeconds = 604_800L;

    @DdcValue(
            value = "${tianquan-shoubing.authorization-code.ttl:60}",
            refreshable = false
    )
    private Long authorizationCodeTtlSeconds = 60L;

    @DdcValue(
            value = "${tianquan-shoubing.login.max-failures:5}",
            refreshable = false
    )
    private Integer maximumLoginFailures = 5;

    @DdcValue(
            value = "${tianquan-shoubing.login.lock-duration:900}",
            refreshable = false
    )
    private Long loginLockDurationSeconds = 900L;

    @DdcValue(
            value = "${tianquan-shoubing.password.max-concurrency:8}",
            refreshable = false
    )
    private Integer passwordMaximumConcurrency = 8;
}
