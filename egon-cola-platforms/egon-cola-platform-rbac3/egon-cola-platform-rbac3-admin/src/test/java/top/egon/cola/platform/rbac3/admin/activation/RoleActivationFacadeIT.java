package top.egon.cola.platform.rbac3.admin.activation;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationCandidateService;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationFacade;
import top.egon.cola.platform.rbac3.admin.config.Rbac3AdminProperties;
import top.egon.cola.platform.rbac3.admin.integration.ddc.AtomicRbac3RuntimePolicy;
import top.egon.cola.platform.rbac3.admin.snapshot.application.SessionSnapshotProjector;
import top.egon.cola.platform.rbac3.core.activation.AuthorizationRuleFacts;
import top.egon.cola.platform.rbac3.core.activation.DsdSetFact;
import top.egon.cola.platform.rbac3.core.activation.EligibleAssignmentFact;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleEdge;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleNode;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleActivationFacadeIT {

    static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");

    @Test
    void publishesChangedSetAndTreatsTheSameCanonicalSetAsIdempotent() {
        var facts = facts(List.of(assignment("101", "10")), List.of());
        var transaction = new InMemoryTransaction();
        var runtime = new RecordingRuntimeStore();
        AtomicRbac3RuntimePolicy policy = policy();
        policy.apply(AtomicRbac3RuntimePolicy.MAXIMUM_ACTIVE_ROOTS_KEY, "1", 1L);
        RoleActivationFacade facade = facade(
                facts, transaction, runtime, policy, new AtomicInteger());

        var first = facade.replace(command(List.of("10"), 0, "command-1"));
        var repeated = facade.replace(command(List.of("10"), 1, "command-2"));

        assertThat(first.changed()).isTrue();
        assertThat(first.sessionVersion()).isEqualTo(1);
        assertThat(first.accessToken()).isEqualTo("token-1");
        assertThat(first.activeRoles()).singleElement()
                .satisfies(app -> assertThat(app.rootRoleIds()).containsExactly("10"));
        assertThat(repeated.changed()).isFalse();
        assertThat(repeated.sessionVersion()).isEqualTo(1);
        assertThat(runtime.fences).hasValue(1);
        assertThat(runtime.publications).hasValue(1);
    }

    @Test
    void dsdDenialLeavesSessionAndRuntimeUnchanged() {
        var facts = facts(
                List.of(assignment("101", "10"), assignment("102", "20")),
                List.of(new DsdSetFact("500", "1", 1, Set.of("10", "20"))));
        var transaction = new InMemoryTransaction();
        var runtime = new RecordingRuntimeStore();
        RoleActivationFacade facade = facade(facts, transaction, runtime);

        assertThatThrownBy(() -> facade.replace(
                command(List.of("10", "20"), 0, "command-denied")))
                .isInstanceOf(Rbac3RuleViolation.class)
                .hasMessageContaining("APP_ROLE_ACTIVATION_MUTEX_VIOLATION");

        assertThat(transaction.sessionVersion).isZero();
        assertThat(transaction.roots).isEmpty();
        assertThat(runtime.fences).hasValue(0);
        assertThat(runtime.publications).hasValue(0);
    }

    @Test
    void countsCanonicalRootsAfterParentAndChildNormalization() {
        RoleHierarchy hierarchy = new RoleHierarchy(
                List.of(role("10", "1", "ROOT_A"), role("11", "1", "CHILD_A")),
                List.of(new RoleEdge("10", "11")));
        var facts = facts(
                hierarchy,
                List.of(assignment("101", "11")),
                List.of(),
                Map.of("1", new RoleActivationCandidateService.ApplicationFact(
                        "1", "finance", "Finance")));
        var transaction = new InMemoryTransaction();
        var runtime = new RecordingRuntimeStore();
        AtomicRbac3RuntimePolicy policy = policy();
        policy.apply(AtomicRbac3RuntimePolicy.MAXIMUM_ACTIVE_ROOTS_KEY, "1", 1L);
        RoleActivationFacade facade = facade(
                facts, transaction, runtime, policy, new AtomicInteger());

        var result = facade.replace(command(List.of("10", "11"), 0, "command-family"));

        assertThat(result.activeRoles()).singleElement()
                .satisfies(app -> assertThat(app.rootRoleIds()).containsExactly("10"));
        assertThat(transaction.roots).containsEntry("1", Set.of("10"));
    }

    @Test
    void rejectsTheNextActivationAboveTheDynamicCanonicalRootLimitBeforeSideEffects() {
        var facts = factsAcrossApplications();
        var transaction = new InMemoryTransaction();
        var runtime = new RecordingRuntimeStore();
        var tokenIssuances = new AtomicInteger();
        AtomicRbac3RuntimePolicy policy = policy();
        policy.apply(AtomicRbac3RuntimePolicy.MAXIMUM_ACTIVE_ROOTS_KEY, "2", 1L);
        RoleActivationFacade facade = facade(
                facts, transaction, runtime, policy, tokenIssuances);

        facade.replace(command(List.of("10", "20"), 0, "command-two-roots"));
        assertThat(transaction.roots).hasSize(2);
        assertThat(tokenIssuances).hasValue(1);
        policy.apply(AtomicRbac3RuntimePolicy.MAXIMUM_ACTIVE_ROOTS_KEY, "1", 2L);

        assertThatThrownBy(() -> facade.replace(
                command(List.of("10", "20"), 1, "command-over-limit")))
                .isInstanceOfSatisfying(Rbac3RuleViolation.class, violation -> {
                    assertThat(violation.reasonCode())
                            .isEqualTo("ACTIVE_ROLE_ROOT_LIMIT_EXCEEDED");
                    assertThat(violation.evidenceIds()).containsExactly("2", "1");
                });

        assertThat(transaction.sessionVersion).isEqualTo(1);
        assertThat(transaction.roots).containsOnly(
                Map.entry("1", Set.of("10")),
                Map.entry("2", Set.of("20")));
        assertThat(runtime.fences).hasValue(1);
        assertThat(runtime.publications).hasValue(1);
        assertThat(tokenIssuances).hasValue(1);
    }

    @Test
    void publicationFailureReturnsStablePendingCodeAndRecordsRecovery() {
        var facts = facts(List.of(assignment("101", "10")), List.of());
        var transaction = new InMemoryTransaction();
        var runtime = new RecordingRuntimeStore(true);
        RoleActivationFacade facade = facade(facts, transaction, runtime);

        assertThatThrownBy(() -> facade.replace(
                command(List.of("10"), 0, "command-pending")))
                .isInstanceOf(Rbac3RuleViolation.class)
                .hasMessageContaining("AUTH_PROPAGATION_PENDING");

        assertThat(transaction.sessionVersion).isEqualTo(1);
        assertThat(transaction.recoveries).hasValue(1);
        assertThat(runtime.fences).hasValue(1);
        assertThat(runtime.publications).hasValue(1);
    }

    @Test
    void criticalRoleActivationRequiresRecentStrongAuthentication() {
        var facts = factsWithRisk(RoleNode.RiskLevel.CRITICAL);
        var transaction = new InMemoryTransaction();
        var runtime = new RecordingRuntimeStore();
        RoleActivationFacade facade = facade(facts, transaction, runtime);

        assertThatThrownBy(() -> facade.replace(
                command(List.of("10"), 0, "command-critical")))
                .isInstanceOf(Rbac3RuleViolation.class)
                .hasMessageContaining("STEP_UP_REQUIRED");

        transaction.authenticationStrength = "STRONG";
        transaction.strongAuthenticatedAt = NOW.minusSeconds(599);
        assertThat(facade.replace(command(List.of("10"), 0, "command-strong"))
                .changed()).isTrue();
    }

    private RoleActivationFacade facade(
            RoleActivationCandidateService.ActivationFacts facts,
            InMemoryTransaction transaction,
            RecordingRuntimeStore runtime
    ) {
        return facade(facts, transaction, runtime, policy(), new AtomicInteger());
    }

    private RoleActivationFacade facade(
            RoleActivationCandidateService.ActivationFacts facts,
            InMemoryTransaction transaction,
            RecordingRuntimeStore runtime,
            AtomicRbac3RuntimePolicy policy,
            AtomicInteger tokenIssuances
    ) {
        return new RoleActivationFacade(
                (tenantId, userId, now) -> facts,
                transaction,
                new SessionSnapshotProjector(),
                runtime,
                (tenantId, userId, sessionId, authVersion, sessionVersion,
                        policyVersion, now) -> {
                    tokenIssuances.incrementAndGet();
                    return new RoleActivationFacade.IssuedToken(
                            "token-" + sessionVersion, now.plusSeconds(900));
                },
                policy,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private RoleActivationFacade.ReplaceCommand command(
            List<String> roots,
            long expectedVersion,
            String commandId
    ) {
        return new RoleActivationFacade.ReplaceCommand(
                "7", "9", "99", roots, expectedVersion, "9", commandId);
    }

    static RoleActivationCandidateService.ActivationFacts facts(
            List<EligibleAssignmentFact> assignments,
            List<DsdSetFact> dsdSets
    ) {
        RoleHierarchy hierarchy = new RoleHierarchy(
                List.of(
                        role("10", "ROOT_A"),
                        role("20", "ROOT_B")),
                List.of());
        return new RoleActivationCandidateService.ActivationFacts(
                "7", "9", hierarchy, assignments, dsdSets,
                new AuthorizationRuleFacts(
                        List.of(), List.of(), List.of(), List.of(), List.of()),
                3, 4, "directory:2",
                Map.of("1", new RoleActivationCandidateService.ApplicationFact(
                        "1", "finance", "Finance")),
                Map.of("10", "Root A", "20", "Root B"));
    }

    private static RoleActivationCandidateService.ActivationFacts facts(
            RoleHierarchy hierarchy,
            List<EligibleAssignmentFact> assignments,
            List<DsdSetFact> dsdSets,
            Map<String, RoleActivationCandidateService.ApplicationFact> applications
    ) {
        Map<String, String> roleNames = hierarchy.nodes().keySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Function.identity(), Function.identity()));
        return new RoleActivationCandidateService.ActivationFacts(
                "7", "9", hierarchy, assignments, dsdSets,
                new AuthorizationRuleFacts(
                        List.of(), List.of(), List.of(), List.of(), List.of()),
                3, 4, "directory:2", applications, roleNames);
    }

    private static RoleActivationCandidateService.ActivationFacts factsAcrossApplications() {
        RoleHierarchy hierarchy = new RoleHierarchy(
                List.of(role("10", "1", "ROOT_A"), role("20", "2", "ROOT_B")),
                List.of());
        return facts(
                hierarchy,
                List.of(assignment("101", "10"), assignment("102", "20")),
                List.of(),
                Map.of(
                        "1", new RoleActivationCandidateService.ApplicationFact(
                                "1", "finance", "Finance"),
                        "2", new RoleActivationCandidateService.ApplicationFact(
                                "2", "reporting", "Reporting")));
    }

    static EligibleAssignmentFact assignment(String id, String roleId) {
        return new EligibleAssignmentFact(
                id, "9", roleId, EligibleAssignmentFact.Status.ACTIVE,
                NOW.minusSeconds(60), null);
    }

    private static RoleActivationCandidateService.ActivationFacts factsWithRisk(
            RoleNode.RiskLevel risk) {
        RoleHierarchy hierarchy = new RoleHierarchy(
                List.of(new RoleNode("10", "1", "ROOT_A", true,
                        risk, false, null, 10)), List.of());
        return new RoleActivationCandidateService.ActivationFacts(
                "7", "9", hierarchy, List.of(assignment("101", "10")), List.of(),
                new AuthorizationRuleFacts(
                        List.of(), List.of(), List.of(), List.of(), List.of()),
                3, 4, "directory:2",
                Map.of("1", new RoleActivationCandidateService.ApplicationFact(
                        "1", "finance", "Finance")),
                Map.of("10", "Root A"));
    }

    private static RoleNode role(String id, String code) {
        return role(id, "1", code);
    }

    private static RoleNode role(String id, String applicationId, String code) {
        return new RoleNode(id, applicationId, code, true,
                RoleNode.RiskLevel.LOW, false, null, 10);
    }

    static AtomicRbac3RuntimePolicy policy() {
        return new AtomicRbac3RuntimePolicy(new Rbac3AdminProperties());
    }

    static final class InMemoryTransaction
            implements RoleActivationFacade.ActivationTransaction {
        private long sessionVersion;
        private Map<String, Set<String>> roots = Map.of();
        private String checksum;
        private String authenticationStrength = "PASSWORD";
        private Instant strongAuthenticatedAt;
        private final AtomicInteger recoveries = new AtomicInteger();

        @Override
        public synchronized RoleActivationFacade.TransactionResult replace(
                RoleActivationFacade.ReplaceCommand command,
                Instant now,
                Function<RoleActivationFacade.SessionState,
                        RoleActivationFacade.ResolvedActivation> factory
        ) {
            if (command.expectedSessionVersion() != sessionVersion) {
                throw new Rbac3RuleViolation("SESSION_VERSION_CONFLICT");
            }
            var resolved = factory.apply(new RoleActivationFacade.SessionState(
                    "7", "9", "99", roots, 3, sessionVersion, 4,
                    checksum, roots.isEmpty(), NOW.plusSeconds(3600),
                    authenticationStrength, strongAuthenticatedAt));
            Map<String, Set<String>> next = resolved.resolution()
                    .activeRoleSet().rootsByApplication();
            boolean changed = !next.equals(roots);
            if (changed) {
                roots = next;
                sessionVersion++;
                checksum = resolved.resolution().snapshot().checksum();
            }
            return new RoleActivationFacade.TransactionResult(
                    resolved, changed, changed ? command.commandId() : null,
                    roots, 3, sessionVersion, 4,
                    checksum, NOW.plusSeconds(3600));
        }

        @Override
        public RoleActivationFacade.CurrentState current(
                String tenantId, String userId, String sessionId, Instant now) {
            return new RoleActivationFacade.CurrentState(
                    roots, 3, sessionVersion, 4, checksum, roots.isEmpty());
        }

        @Override
        public void markRecoveryRequired(
                String mutationId, String reasonCode, Instant now) {
            recoveries.incrementAndGet();
        }
    }

    static final class RecordingRuntimeStore implements RoleActivationFacade.RuntimeStore {
        private final AtomicInteger fences = new AtomicInteger();
        private final AtomicInteger publications = new AtomicInteger();
        private final boolean failPublication;

        RecordingRuntimeStore() {
            this(false);
        }

        RecordingRuntimeStore(boolean failPublication) {
            this.failPublication = failPublication;
        }

        @Override
        public void createFence(
                String tenantId, String sessionId, String mutationId,
                java.time.Duration ttl) {
            fences.incrementAndGet();
        }

        @Override
        public void publish(RoleActivationFacade.RuntimePublication publication) {
            publications.incrementAndGet();
            if (failPublication) {
                throw new IllegalStateException("redis unavailable");
            }
        }
    }
}
