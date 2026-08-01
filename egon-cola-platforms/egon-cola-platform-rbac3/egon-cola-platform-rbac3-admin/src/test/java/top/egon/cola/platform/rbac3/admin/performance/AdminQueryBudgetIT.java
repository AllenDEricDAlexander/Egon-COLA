package top.egon.cola.platform.rbac3.admin.performance;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationCandidateService;
import top.egon.cola.platform.rbac3.admin.audit.application.AuditQueryService;
import top.egon.cola.platform.rbac3.core.activation.AuthorizationRuleFacts;
import top.egon.cola.platform.rbac3.core.activation.EligibleAssignmentFact;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleNode;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminQueryBudgetIT {

    private static final Instant NOW = Instant.parse("2026-07-30T08:00:00Z");

    @Test
    void candidateAndAuditPagesUseOneBulkReadEach() {
        AtomicInteger candidateLoads = new AtomicInteger();
        RoleActivationCandidateService candidates = new RoleActivationCandidateService(
                (tenant, user, at) -> {
                    candidateLoads.incrementAndGet();
                    return candidateFacts();
                });
        candidates.candidates("tenant", "user", NOW);
        assertEquals(1, candidateLoads.get());

        CountingAuditStore auditStore = new CountingAuditStore();
        AuditQueryService audit = new AuditQueryService(
                auditStore, Clock.fixed(NOW, ZoneOffset.UTC));
        audit.query(new AuditQueryService.Query(
                        "tenant", NOW.minusSeconds(60), NOW,
                        null, null, null, null, null, null, null, null,
                        100, null),
                "auditor", "request", "trace");
        assertEquals(1, auditStore.queries.get());
        assertEquals(1, auditStore.appends.get());
    }

    @Test
    void calibratedEnvironmentMayEnforceCandidateReadBudget() {
        Assumptions.assumeTrue(Boolean.getBoolean("rbac3.performance.enforce"));
        RoleActivationCandidateService service = new RoleActivationCandidateService(
                (tenant, user, at) -> candidateFacts());

        Instant started = Instant.now();
        for (int index = 0; index < 1_000; index++) {
            service.candidates("tenant", "user", NOW);
        }

        assertTrue(Duration.between(started, Instant.now())
                .compareTo(Duration.ofSeconds(2)) < 0);
    }

    private static RoleActivationCandidateService.ActivationFacts candidateFacts() {
        RoleNode root = new RoleNode(
                "root", "application", "ROOT", true,
                RoleNode.RiskLevel.LOW, false, null, 100);
        return new RoleActivationCandidateService.ActivationFacts(
                "tenant", "user", new RoleHierarchy(List.of(root), List.of()),
                List.of(new EligibleAssignmentFact(
                        "assignment", "user", "root",
                        EligibleAssignmentFact.Status.ACTIVE,
                        NOW.minusSeconds(60), null)),
                List.of(),
                new AuthorizationRuleFacts(
                        List.of(), List.of(), List.of(), List.of(), List.of()),
                3L, 7L, "directory:1",
                Map.of("application", new RoleActivationCandidateService.ApplicationFact(
                        "application", "finance", "Finance")),
                Map.of("root", "Finance root"));
    }

    private static final class CountingAuditStore
            implements AuditQueryService.AuditStore {
        private final AtomicInteger queries = new AtomicInteger();
        private final AtomicInteger appends = new AtomicInteger();

        @Override
        public AuditQueryService.AuditView append(
                AuditQueryService.AuditView record) {
            appends.incrementAndGet();
            return record;
        }

        @Override
        public AuditQueryService.Page query(AuditQueryService.Query query) {
            queries.incrementAndGet();
            return new AuditQueryService.Page(List.of(), null);
        }
    }
}
