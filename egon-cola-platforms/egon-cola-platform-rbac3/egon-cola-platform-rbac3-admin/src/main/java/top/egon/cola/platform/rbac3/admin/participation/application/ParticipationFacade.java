package top.egon.cola.platform.rbac3.admin.participation.application;

import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3ServicePrincipal;
import top.egon.cola.platform.rbac3.contract.participation.BusinessParticipationCommand;
import top.egon.cola.platform.rbac3.core.participation.OperationSodSpecification;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Enforces application binding and same-object duty separation before append.
 */
public final class ParticipationFacade {

    private final OperationSodRuleSource ruleSource;
    private final ParticipationStore store;
    private final Clock clock;
    private final OperationSodSpecification specification = new OperationSodSpecification();

    public ParticipationFacade(
            OperationSodRuleSource ruleSource,
            ParticipationStore store,
            Clock clock) {
        this.ruleSource = Objects.requireNonNull(ruleSource, "ruleSource");
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public RecordResult record(
            CurrentRbac3ServicePrincipal caller,
            String tenantId,
            BusinessParticipationCommand command) {
        requireBinding(caller, tenantId, command.applicationCode());
        List<PriorActionRule> rules = ruleSource.rules(
                tenantId, command.applicationCode(), command.businessResource(),
                command.actionCode(), clock.instant());
        ParticipationRecord record = new ParticipationRecord(
                tenantId, command.applicationCode(), command.businessResource(),
                command.businessId(), command.actorUserId(), command.actionCode(),
                command.businessEventId(), command.occurredAt(), command.traceId(),
                digest(tenantId, command));
        AppendResult result = store.appendAtomically(record, rules);
        if (!result.conflictingEvidenceIds().isEmpty()) {
            throw new Rbac3RuleViolation(
                    "OPERATION_SOD_VIOLATION", result.conflictingEvidenceIds());
        }
        return new RecordResult(
                result.created(), result.participationId(),
                result.created() ? "CREATED" : "IDEMPOTENT_REPLAY");
    }

    public ConflictDecision conflicts(
            CurrentRbac3ServicePrincipal caller,
            String tenantId,
            ConflictQuery query) {
        requireBinding(caller, tenantId, query.applicationCode());
        List<PriorActionRule> rules = ruleSource.rules(
                tenantId, query.applicationCode(), query.businessResource(),
                query.requestedAction(), clock.instant());
        Instant lookbackFrom = rules.stream()
                .map(PriorActionRule::lookbackFrom)
                .min(Instant::compareTo)
                .orElse(clock.instant());
        List<ParticipationFact> facts = store.find(query, tenantId, lookbackFrom).stream()
                .filter(fact -> rules.stream().anyMatch(rule ->
                        rule.actionCode().equals(fact.actionCode())
                                && !fact.occurredAt().isBefore(rule.lookbackFrom())))
                .toList();
        Set<String> forbidden = rules.stream()
                .map(PriorActionRule::actionCode)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        var coreFacts = facts.stream()
                .map(fact -> new OperationSodSpecification.ParticipationFact(
                        fact.participationId(), fact.businessResource(), fact.businessId(),
                        fact.actorUserId(), fact.actionCode()))
                .toList();
        var result = specification.evaluate(
                query.businessResource(), query.businessId(), query.actorUserId(),
                query.requestedAction(), coreFacts,
                Map.of(query.requestedAction(), forbidden));
        return new ConflictDecision(
                result.allowed(), result.reasonCode(), result.evidenceIds(), forbidden);
    }

    private void requireBinding(
            CurrentRbac3ServicePrincipal caller,
            String tenantId,
            String applicationCode) {
        Objects.requireNonNull(caller, "caller");
        if (!caller.tenantId().equals(tenantId)) {
            throw new Rbac3RuleViolation("SERVICE_IDENTITY_DENIED");
        }
        if (!caller.applicationCode().equals(applicationCode)) {
            throw new Rbac3RuleViolation("APPLICATION_BINDING_DENIED");
        }
    }

    private String digest(String tenantId, BusinessParticipationCommand command) {
        String canonical = String.join("\u001f",
                tenantId, command.applicationCode(), command.businessResource(),
                command.businessId(), command.actorUserId(), command.actionCode(),
                command.businessEventId(), command.occurredAt().toString(), command.traceId());
        try {
            return "sha256:" + HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    @FunctionalInterface
    public interface OperationSodRuleSource {
        List<PriorActionRule> rules(
                String tenantId,
                String applicationCode,
                String businessResource,
                String laterAction,
                Instant at);
    }

    public interface ParticipationStore {
        AppendResult appendAtomically(
                ParticipationRecord record,
                List<PriorActionRule> rules);

        List<ParticipationFact> find(
                ConflictQuery query,
                String tenantId,
                Instant lookbackFrom);
    }

    public record ParticipationRecord(
            String tenantId,
            String applicationCode,
            String businessResource,
            String businessId,
            String actorUserId,
            String actionCode,
            String businessEventId,
            Instant occurredAt,
            String traceId,
            String payloadDigest) {
    }

    public record ParticipationFact(
            String participationId,
            String tenantId,
            String applicationCode,
            String businessResource,
            String businessId,
            String actorUserId,
            String actionCode,
            String businessEventId,
            Instant occurredAt) {
    }

    public record PriorActionRule(
            String ruleId,
            String actionCode,
            Instant lookbackFrom) {
        public PriorActionRule {
            if (ruleId == null || ruleId.isBlank()) {
                throw new IllegalArgumentException("ruleId is required");
            }
            if (actionCode == null || actionCode.isBlank()) {
                throw new IllegalArgumentException("actionCode is required");
            }
            lookbackFrom = Objects.requireNonNull(lookbackFrom, "lookbackFrom");
        }
    }

    public record AppendResult(
            boolean created,
            String participationId,
            List<String> conflictingEvidenceIds) {
        public AppendResult {
            conflictingEvidenceIds = List.copyOf(conflictingEvidenceIds);
        }
    }

    public record RecordResult(
            boolean created,
            String participationId,
            String reasonCode) {
    }

    public record ConflictQuery(
            String applicationCode,
            String businessResource,
            String businessId,
            String actorUserId,
            String requestedAction) {
    }

    public record ConflictDecision(
            boolean allowed,
            String reasonCode,
            List<String> evidenceIds,
            Set<String> conflictingPriorActions) {
    }
}
