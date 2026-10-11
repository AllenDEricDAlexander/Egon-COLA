package top.egon.cola.platform.tianquan.jianshen.yuheng.security;

import org.reactivestreams.Publisher;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.yuheng.core.security.AuthorizationDecision;
import top.egon.cola.component.yuheng.core.security.GatewayAuthContext;
import top.egon.cola.component.yuheng.core.security.GatewayAuthorizationProvider;
import top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpOAuthScopeAuthorizationProvider;

import java.util.Objects;

/**
 * Adapts the blocking Tianquan-Jianshen BIZ/APP scope reader to the reactive Gateway
 * authorization SPI and fails closed when the scope runtime is unavailable.
 */
@Slf4j
@RequiredArgsConstructor
public final class Rbac3BizAppScopeAuthorizationProvider
        implements GatewayAuthorizationProvider {

    public static final String PROVIDER_ID = "tianquan-jianshen-biz-app-scope";

    @Qualifier("rbac3GatewayScopeSnapshotReader") private final DecisionSource decisionSource;
    @Qualifier("idpObservedVerificationComponent") private final IdpObservedVerificationComponent observations;
    @Qualifier("idpSecurityMetricsRecorder") private final IdpSecurityMetricsRecorder metrics;
    @Qualifier("defaultValidator") private final Validator validator;

    public Rbac3BizAppScopeAuthorizationProvider(DecisionSource decisionSource) {
        this(Objects.requireNonNull(decisionSource, "decisionSource"), null, IdpSecurityMetricsRecorder.NOOP, null);
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public Publisher<AuthorizationDecision> authorize(GatewayAuthContext context) {
        return Mono.fromCallable(() -> decisionSource.authorize(context))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorReturn(AuthorizationDecision.error(
                        "TIANQUAN_JIANSHEN_SCOPE_RUNTIME_UNAVAILABLE"))
                .flatMap(decision -> IdpOAuthScopeAuthorizationProvider.observeDecision(context, decision, observations, metrics, validator));
    }

    @FunctionalInterface
    public interface DecisionSource {
        AuthorizationDecision authorize(GatewayAuthContext context);
    }
}
