package top.egon.cola.platform.rbac3.admin.activation;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationCandidateService;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationFacade;
import top.egon.cola.platform.rbac3.admin.snapshot.application.SessionSnapshotProjector;
import top.egon.cola.platform.rbac3.core.activation.AuthorizationRuleFacts;
import top.egon.cola.platform.rbac3.core.activation.DsdSetFact;
import top.egon.cola.platform.rbac3.core.activation.EligibleAssignmentFact;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
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
        RoleActivationFacade facade = facade(facts, transaction, runtime);

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

    private RoleActivationFacade facade(
            RoleActivationCandidateService.ActivationFacts facts,
            InMemoryTransaction transaction,
            RecordingRuntimeStore runtime
    ) {
        return new RoleActivationFacade(
                (tenantId, userId, now) -> facts,
                transaction,
                new SessionSnapshotProjector(),
                runtime,
                (tenantId, userId, sessionId, authVersion, sessionVersion,
                        policyVersion, now) -> new RoleActivationFacade.IssuedToken(
                        "token-" + sessionVersion, now.plusSeconds(900)),
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

    static EligibleAssignmentFact assignment(String id, String roleId) {
        return new EligibleAssignmentFact(
                id, "9", roleId, EligibleAssignmentFact.Status.ACTIVE,
                NOW.minusSeconds(60), null);
    }

    private static RoleNode role(String id, String code) {
        return new RoleNode(id, "1", code, true,
                RoleNode.RiskLevel.LOW, false, null, 10);
    }

    static final class InMemoryTransaction
            implements RoleActivationFacade.ActivationTransaction {
        private long sessionVersion;
        private Map<String, Set<String>> roots = Map.of();
        private String checksum;
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
                    checksum, roots.isEmpty(), NOW.plusSeconds(3600)));
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
