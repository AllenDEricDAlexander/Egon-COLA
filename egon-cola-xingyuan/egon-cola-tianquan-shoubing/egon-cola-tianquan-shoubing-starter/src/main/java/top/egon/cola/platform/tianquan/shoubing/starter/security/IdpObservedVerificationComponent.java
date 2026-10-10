package top.egon.cola.platform.tianquan.shoubing.starter.security;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Validator;
import java.net.URI;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.platform.tianquan.shoubing.contract.IdpPrincipal;
import top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder;
import top.egon.cola.platform.tianquan.shoubing.core.observability.SecurityMetricObservationBO;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditContextBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.VerifiedJwtBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverter;

/** Decorates one completed verification with a strict, secret-free usage handoff. */
@Slf4j
@RequiredArgsConstructor
public class IdpObservedVerificationComponent {
    @Qualifier("tokenUsageAuditComponent") private final TokenUsageAuditComponent producer;
    @Qualifier("tokenUsageAuditEventConverterImpl") private final TokenUsageAuditEventConverter converter;
    @Qualifier("oauthUsageObservationProperties") private final OAuthUsageObservationProperties properties;
    @Qualifier("idpSecurityMetricsRecorder") private final IdpSecurityMetricsRecorder metrics;
    @Qualifier("idpClock") private final Clock clock;
    @Qualifier("defaultValidator") private final Validator validator;
    private final URI resource;
    private final AtomicBoolean metricFailureLogged = new AtomicBoolean();
    private final AtomicBoolean deliveryFailureLogged = new AtomicBoolean();
    private final AtomicBoolean proofFailureLogged = new AtomicBoolean();

    @PostConstruct
    void validateConfiguration() {
        new ValidationUtils(validator).validate(properties);
        if (properties.isEnabled()) {
            new TokenAuditProofBO(properties.getFallbackIssuer(), resource, null, null, null);
            if (metrics == IdpSecurityMetricsRecorder.NOOP) {
                throw new IllegalStateException("OAUTH_USAGE_METRICS_REQUIRED");
            }
        }
    }

    public boolean enabled() { return properties.isEnabled(); }

    public <P extends IdpPrincipal> AccessTokenVerification<P> observe(
            Function<Consumer<VerifiedJwtBO>, AccessTokenVerification<P>> originalVerification) {
        java.util.Objects.requireNonNull(originalVerification, "originalVerification");
        if (!properties.isEnabled()) { return originalVerification.apply(ignored -> { }); }
        AtomicReference<VerifiedJwtBO> captured = new AtomicReference<>();
        try {
            AccessTokenVerification<P> result = java.util.Objects.requireNonNull(originalVerification.apply(captured::set), "verification");
            TokenAuditProofBO proof = null;
            if (result instanceof AccessTokenVerification.Valid<?>) {
                if (captured.get() == null) {
                    result = new AccessTokenVerification.Unavailable<>("OAUTH_AUDIT_PROOF_UNAVAILABLE");
                } else {
                    try {
                        captured.get().validate();
                        proof = new ValidationUtils(validator).validate(converter.toProof(
                                new ValidationUtils(validator).validate(captured.get()), resource, producer));
                    } catch (RuntimeException incomplete) {
                        if (proofFailureLogged.compareAndSet(false, true)) {
                            log.warn("Trusted usage proof unavailable; failureType={}", incomplete.getClass().getSimpleName());
                        }
                        result = new AccessTokenVerification.Unavailable<>("OAUTH_AUDIT_PROOF_UNAVAILABLE");
                    }
                }
            }
            metric("oauth.authentication", category(result));
            if (!handoff(proof, proof == null ? "FAILED" : "SUCCESS",
                    proof == null ? "AUTHENTICATION_FAILED" : "TOKEN_USE_VERIFIED")) {
                return new AccessTokenVerification.Unavailable<>("OAUTH_AUDIT_DELIVERY_UNAVAILABLE");
            }
            if (result instanceof AccessTokenVerification.Valid<P> valid) {
                return new AccessTokenVerification.Valid<>(valid.principal(), valid.scopes(), proof);
            }
            return result;
        } finally {
            captured.set(null);
        }
    }

    /** Anonymous pre-render decision; no unverified claim is consulted. */
    public boolean authenticationFailure() {
        if (!properties.isEnabled()) { return true; }
        metric("oauth.authentication", "INVALID");
        return handoff(null, "FAILED", "AUTHENTICATION_FAILED");
    }

    /** A separate authorization decision is emitted only for a proven token-bearing call. */
    public boolean authorizationDenied(@jakarta.validation.Valid TokenAuditProofBO proof) {
        if (!properties.isEnabled() || proof == null) { return true; }
        return handoff(new ValidationUtils(validator).validate(proof), "DENIED", "AUTHORIZATION_DENIED");
    }

    private boolean handoff(TokenAuditProofBO proof, String result, String reason) {
        try {
            String trace = MDC.get("traceId");
            if (trace != null && !trace.matches("[A-Za-z0-9._~-]{1,128}")) { trace = null; }
            TokenAuditContextBO context = TokenAuditContextBO.builder().proof(proof)
                    .eventId(UUID.randomUUID().toString().replace("-", ""))
                    .fallbackIssuer(properties.getFallbackIssuer()).resource(resource).result(result).reasonCode(reason)
                    .occurredAt(clock.instant()).traceId(trace).build();
            new ValidationUtils(validator).validate(context);
            producer.appendStrict(new ValidationUtils(validator).validate(converter.toTarget(context)));
            metric("oauth.audit.delivery", "SENT");
            return true;
        } catch (RuntimeException failure) {
            metric("oauth.audit.delivery", "FAILED");
            if (deliveryFailureLogged.compareAndSet(false, true)) {
                log.warn("Strict usage handoff unavailable; failureType={}", failure.getClass().getSimpleName());
            }
            return false;
        }
    }

    private String category(AccessTokenVerification<?> result) {
        if (result instanceof AccessTokenVerification.Valid<?>) { return "VALID"; }
        if (result instanceof AccessTokenVerification.Expired<?>) { return "EXPIRED"; }
        if (result instanceof AccessTokenVerification.Unavailable<?>) { return "UNAVAILABLE"; }
        return "INVALID";
    }

    private void metric(String signal, String result) {
        try {
            metrics.record(SecurityMetricObservationBO.builder().signal(signal).result(result).build());
        } catch (RuntimeException failure) {
            if (metricFailureLogged.compareAndSet(false, true)) {
                log.warn("Usage metric unavailable; failureType={}", failure.getClass().getSimpleName());
            }
        }
    }
}
