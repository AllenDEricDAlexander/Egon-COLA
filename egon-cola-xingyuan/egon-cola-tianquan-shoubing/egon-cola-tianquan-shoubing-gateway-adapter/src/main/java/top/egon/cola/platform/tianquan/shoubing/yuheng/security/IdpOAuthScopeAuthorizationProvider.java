package top.egon.cola.platform.tianquan.shoubing.yuheng.security;

import jakarta.validation.Validator;
import java.util.AbstractMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.ContextView;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.yuheng.core.security.AuthorizationDecision;
import top.egon.cola.component.yuheng.core.security.GatewayAuthContext;
import top.egon.cola.component.yuheng.core.security.GatewayAuthorizationProvider;
import top.egon.cola.component.yuheng.core.security.SecurityDecision;
import top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder;
import top.egon.cola.platform.tianquan.shoubing.core.observability.SecurityMetricObservationBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.IdpAuthorizationObservationContextBO;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.domain.GatewayVerifiedIdentityBO;

/** Mandatory scope strategy; only the same verified evidence and signed compiled policy are authoritative. */
@Slf4j
@RequiredArgsConstructor
public class IdpOAuthScopeAuthorizationProvider implements GatewayAuthorizationProvider {
    public static final String PROVIDER_ID = "tianquan-shoubing-oauth-scope";
    @Qualifier("idpObservedVerificationComponent") private final IdpObservedVerificationComponent observations;
    @Qualifier("idpSecurityMetricsRecorder") private final IdpSecurityMetricsRecorder metrics;
    @Qualifier("defaultValidator") private final Validator validator;

    @Override public String providerId() { return PROVIDER_ID; }
    @Override public boolean mandatory() { return true; }

    @Override
    public Publisher<AuthorizationDecision> authorize(GatewayAuthContext context) {
        return Mono.deferContextual(reactor -> Mono.fromCallable(() -> {
            if (!context.principal().authenticated()
                    || !(context.principal().trustedAuthenticationEvidence() instanceof GatewayVerifiedIdentityBO evidence)
                    || evidence.proof() == null || observationContext(reactor) == null) {
                return AuthorizationDecision.error("GATEWAY_VERIFIED_SCOPE_CONTEXT_UNAVAILABLE");
            }
            new ValidationUtils(validator).validate(evidence);
            if (!context.requiredOAuthScopes().isEmpty() && evidence.verifiedScopes().containsAll(context.requiredOAuthScopes())) {
                record(metrics, "ALLOW");
                return AuthorizationDecision.allow();
            }
            return denied(context, reactor, AuthorizationDecision.deny("OAUTH_SCOPE_REQUIRED"), observations, metrics, validator);
        }).subscribeOn(Schedulers.boundedElastic())).onErrorReturn(AuthorizationDecision.error("OAUTH_AUDIT_DELIVERY_UNAVAILABLE"));
    }

    /** Reuses the physical receiver's request marker for ordinary authorization decisions. */
    public static Mono<AuthorizationDecision> observeDecision(GatewayAuthContext context, AuthorizationDecision decision,
            IdpObservedVerificationComponent observations, IdpSecurityMetricsRecorder metrics, Validator validator) {
        if (decision.decision() == SecurityDecision.ALLOW || observations == null) { return Mono.just(decision); }
        return Mono.deferContextual(reactor -> Mono.fromCallable(() -> denied(context, reactor, decision, observations, metrics, validator))
                .subscribeOn(Schedulers.boundedElastic())).onErrorReturn(AuthorizationDecision.error("OAUTH_AUDIT_DELIVERY_UNAVAILABLE"));
    }

    private static AuthorizationDecision denied(GatewayAuthContext context, ContextView reactor, AuthorizationDecision decision,
            IdpObservedVerificationComponent observations, IdpSecurityMetricsRecorder metrics, Validator validator) {
        var marker = observationContext(reactor);
        if (marker == null || !(context.principal().trustedAuthenticationEvidence() instanceof GatewayVerifiedIdentityBO evidence)
                || evidence.proof() == null || observations == null) {
            return AuthorizationDecision.error("OAUTH_AUDIT_DELIVERY_UNAVAILABLE");
        }
        new ValidationUtils(validator).validate(evidence);
        record(metrics, decision.decision() == SecurityDecision.ERROR ? "INDETERMINATE" : "DENY");
        return marker.denyOnce(() -> observations.authorizationDenied(evidence.proof(), context.traceId()))
                ? decision : AuthorizationDecision.error("OAUTH_AUDIT_DELIVERY_UNAVAILABLE");
    }

    /** Core carries a JDK request-scope factory; it never imports or constructs the concrete marker. */
    @SuppressWarnings("unchecked")
    public static IdpAuthorizationObservationContextBO observationContext(ContextView context) {
        Object value = context.getOrDefault(GatewayAuthContext.class, null);
        if (!(value instanceof BiFunction<?, ?, ?>)) { return null; }
        var scope = (BiFunction<Object, Supplier<Map.Entry<Object, Runnable>>, Object>) value;
        Object state = scope.apply(IdpAuthorizationObservationContextBO.class, () -> {
            var marker = new IdpAuthorizationObservationContextBO();
            return new AbstractMap.SimpleImmutableEntry<>(marker, marker::close);
        });
        return state instanceof IdpAuthorizationObservationContextBO marker && marker.isActive() ? marker : null;
    }

    private static void record(IdpSecurityMetricsRecorder metrics, String result) {
        try { metrics.record(SecurityMetricObservationBO.builder().signal("rbac.authorization").result(result).build()); }
        catch (RuntimeException unavailable) { log.debug("Gateway authorization metric unavailable; failureType={}", unavailable.getClass().getSimpleName()); }
    }
}
