package top.egon.cola.platform.tianquan.jianshen.gateway.security;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.yuheng.core.security.AuthorizationDecision;
import top.egon.cola.component.yuheng.core.security.GatewayAuthContext;
import top.egon.cola.component.yuheng.core.security.GatewayAuthorizationProvider;

import java.util.Objects;

/**
 * Adapts the blocking Tianquan-Jianshen BIZ/APP scope reader to the reactive Gateway
 * authorization SPI and fails closed when the scope runtime is unavailable.
 */
public final class Rbac3BizAppScopeAuthorizationProvider
        implements GatewayAuthorizationProvider {

    public static final String PROVIDER_ID = "tianquan-jianshen-biz-app-scope";

    private final DecisionSource decisionSource;

    public Rbac3BizAppScopeAuthorizationProvider(DecisionSource decisionSource) {
        this.decisionSource = Objects.requireNonNull(decisionSource, "decisionSource");
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
                        "TIANQUAN_JIANSHEN_SCOPE_RUNTIME_UNAVAILABLE"));
    }

    @FunctionalInterface
    public interface DecisionSource {
        AuthorizationDecision authorize(GatewayAuthContext context);
    }
}
