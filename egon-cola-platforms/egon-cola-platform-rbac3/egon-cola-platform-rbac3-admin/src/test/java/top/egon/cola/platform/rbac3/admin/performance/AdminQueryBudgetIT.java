package top.egon.cola.platform.rbac3.admin.performance;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.iam.role.activation.service.RoleActivationCandidateService;
import top.egon.cola.platform.rbac3.admin.audit.service.AuditQueryService;
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
import top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.vo.ActivationFactsVO;
import top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.vo.ApplicationFactVO;
import top.egon.cola.platform.rbac3.admin.audit.repository.AuditRepository;
import top.egon.cola.platform.rbac3.admin.audit.domain.dto.QueryDTO;
import top.egon.cola.platform.rbac3.admin.audit.domain.vo.AuditVO;
import top.egon.cola.platform.rbac3.admin.audit.domain.vo.AuditQueryPageVO;

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
        audit.query(new QueryDTO(
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

    private static ActivationFactsVO candidateFacts() {
        RoleNode root = new RoleNode(
                "root", "application", "ROOT", true,
                RoleNode.RiskLevel.LOW, false, null, 100);
        return new ActivationFactsVO(
                "tenant", "user", new RoleHierarchy(List.of(root), List.of()),
                List.of(new EligibleAssignmentFact(
                        "assignment", "user", "root",
                        EligibleAssignmentFact.Status.ACTIVE,
                        NOW.minusSeconds(60), null)),
                List.of(),
                new AuthorizationRuleFacts(
                        List.of(), List.of(), List.of(), List.of(), List.of()),
                3L, 7L, "directory:1",
                Map.of("application", new ApplicationFactVO(
                        "application", "finance", "Finance")),
                Map.of("root", "Finance root"));
    }

    private static final class CountingAuditStore
            implements AuditRepository {
        private final AtomicInteger queries = new AtomicInteger();
        private final AtomicInteger appends = new AtomicInteger();

        @Override
        public AuditVO append(
                AuditVO record) {
            appends.incrementAndGet();
            return record;
        }

        @Override
        public AuditQueryPageVO query(QueryDTO query) {
            queries.incrementAndGet();
            return new AuditQueryPageVO(List.of(), null);
        }
    }
}
