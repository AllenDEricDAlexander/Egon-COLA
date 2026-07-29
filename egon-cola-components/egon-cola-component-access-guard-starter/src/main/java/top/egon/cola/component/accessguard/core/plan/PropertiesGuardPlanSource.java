package top.egon.cola.component.accessguard.core.plan;

import top.egon.cola.component.accessguard.core.failure.FailurePoint;
import top.egon.cola.component.accessguard.core.failure.FailurePolicy;
import top.egon.cola.component.accessguard.execution.RejectionMode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class PropertiesGuardPlanSource implements GuardPlanSource {

    private final GuardPlanProperties properties;

    public PropertiesGuardPlanSource(GuardPlanProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() {
        return "properties";
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public Optional<GuardPlanSnapshot> current(String ruleId) {
        GuardPlanProperties.Rule rule = properties.getRules().get(ruleId);
        if (rule == null) {
            return Optional.empty();
        }
        GuardPlan plan = toPlan(ruleId, rule);
        String fingerprint = fingerprint(plan, properties.getKey().getHmacSecret());
        GuardPlan versionedPlan = new GuardPlan(
                plan.id(), plan.enabled(), plan.key(), plan.admission(), plan.execution(),
                plan.failurePolicies(), plan.observability(), fingerprint);
        return Optional.of(new GuardPlanSnapshot(
                ruleId, 0L, Instant.now(), name(), versionedPlan, fingerprint));
    }

    @Override
    public AutoCloseable subscribe(Consumer<GuardPlanSnapshot> listener) {
        return () -> {
        };
    }

    private GuardPlan toPlan(String ruleId, GuardPlanProperties.Rule rule) {
        List<String> contributors = rule.getKey().getContributors().isEmpty()
                ? properties.getKey().getContributors()
                : rule.getKey().getContributors();
        KeyConfig key = new KeyConfig(
                contributors,
                properties.getKey().getTrustedProxies(),
                properties.getKey().getHmacSecret());
        AdmissionConfig admission = new AdmissionConfig(
                new AdmissionConfig.DenyListConfig(rule.getDenyList().isEnabled()),
                new AdmissionConfig.AllowListConfig(
                        rule.getAllowList().isEnabled(), rule.getAllowList().getMode()),
                new AdmissionConfig.PenaltyBoxConfig(
                        rule.getPenaltyBox().isEnabled(),
                        rule.getPenaltyBox().getThreshold(),
                        rule.getPenaltyBox().getViolationTtl(),
                        rule.getPenaltyBox().getPenaltyTtl()),
                new AdmissionConfig.RateLimitConfig(
                        rule.getRateLimit().isEnabled(),
                        rule.getRateLimit().getAlgorithm(),
                        rule.getRateLimit().getCapacity(),
                        rule.getRateLimit().getRefillTokens(),
                        rule.getRateLimit().getRefillPeriod(),
                        rule.getRateLimit().getRequestedTokens()));
        RejectionMode rejectionMode = rule.getRejection().getMode() == null
                ? properties.getDefaults().getRejection()
                : rule.getRejection().getMode();
        ExecutionConfig execution = new ExecutionConfig(
                new ExecutionConfig.TimeLimitConfig(
                        rule.getTimeLimit().isEnabled(),
                        rule.getTimeLimit().getMode(),
                        rule.getTimeLimit().getExecutor(),
                        rule.getTimeLimit().getTimeout(),
                        rule.getTimeLimit().isCancelRunningTask()),
                new ExecutionConfig.RejectionConfig(
                        rejectionMode,
                        rule.getRejection().getFallbackMethod(),
                        rule.getRejection().getReturnJson()));
        GuardPlanProperties.Failures failure = rule.getFailurePolicies();
        EnumMap<FailurePoint, FailurePolicy> policies = new EnumMap<>(FailurePoint.class);
        policies.put(FailurePoint.KEY_RESOLUTION, failure.getKeyResolution());
        policies.put(FailurePoint.DENY_LIST_STORE, failure.getDenyListStore());
        policies.put(FailurePoint.ALLOW_LIST_STORE, failure.getAllowListStore());
        policies.put(FailurePoint.PENALTY_STORE, failure.getPenaltyStore());
        policies.put(FailurePoint.RATE_LIMIT_BACKEND, failure.getRateLimitBackend());
        policies.put(FailurePoint.EXECUTION, failure.getExecution());
        policies.put(FailurePoint.OBSERVABILITY, failure.getObservability());
        GuardPlanProperties.Observability observability = rule.getObservability();
        return new GuardPlan(
                ruleId,
                rule.isEnabled(),
                key,
                admission,
                execution,
                new FailurePolicies(policies),
                new ObservabilityConfig(
                        observability.isFinalEvents(),
                        observability.isStageEvents(),
                        observability.isMetrics(),
                        observability.isLogging(),
                        observability.isEndpoint()),
                "pending");
    }

    private static String fingerprint(GuardPlan plan, String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(plan.toString().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(secret.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
