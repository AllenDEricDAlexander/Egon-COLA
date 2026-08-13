package top.egon.cola.platform.rbac3.admin.participation;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.idp.starter.security.RequiresServiceScope;
import top.egon.cola.platform.rbac3.admin.participation.controller.ParticipationController;
import top.egon.cola.platform.rbac3.admin.participation.service.ParticipationFacade;
import top.egon.cola.platform.rbac3.contract.participation.BusinessParticipationCommand;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import top.egon.cola.platform.rbac3.admin.participation.repository.ParticipationRepository;
import top.egon.cola.platform.rbac3.admin.participation.domain.vo.ParticipationRecordVO;
import top.egon.cola.platform.rbac3.admin.participation.domain.vo.ParticipationFactVO;
import top.egon.cola.platform.rbac3.admin.participation.domain.vo.PriorActionRuleVO;
import top.egon.cola.platform.rbac3.admin.participation.domain.vo.AppendResultVO;
import top.egon.cola.platform.rbac3.admin.participation.domain.vo.RecordResultVO;
import top.egon.cola.platform.rbac3.admin.participation.domain.dto.ConflictQueryDTO;

class ParticipationConcurrencyIT {

    private static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");

    @Test
    void participationConflictSurvivesSessionAndRoleChanges() {
        InMemoryStore store = new InMemoryStore();
        ParticipationFacade facade = facade(store);

        facade.record(caller("finance-service"), "tenant-1", command("SUBMIT", "event-1"));

        var conflict = facade.conflicts(
                caller("finance-service"), "tenant-1",
                new ConflictQueryDTO(
                        "finance-service", "PAYMENT", "PAY-1", "user-1", "APPROVE"));
        assertThat(conflict.allowed()).isFalse();
        assertThat(conflict.reasonCode()).isEqualTo("OPERATION_SOD_CONSTRAINT_VIOLATION");
        assertThat(conflict.evidenceIds()).containsExactly("participation-1");
        assertThatThrownBy(() -> facade.record(
                caller("finance-service"), "tenant-1", command("APPROVE", "event-2")))
                .isInstanceOfSatisfying(Rbac3RuleViolation.class,
                        error -> assertThat(error.reasonCode())
                                .isEqualTo("OPERATION_SOD_VIOLATION"));
    }

    @Test
    void participationEndpointsDeclareIdpOwnedServiceScopes()
            throws NoSuchMethodException {
        assertThat(ParticipationController.class.getMethod(
                        "record", BusinessParticipationCommand.class,
                        ServiceIdentityPrincipal.class)
                .getAnnotation(RequiresServiceScope.class).value())
                .isEqualTo("service:participation:write");
        assertThat(ParticipationController.class.getMethod(
                        "conflicts", String.class, String.class, String.class,
                        String.class, String.class, ServiceIdentityPrincipal.class)
                .getAnnotation(RequiresServiceScope.class).value())
                .isEqualTo("service:participation:read");
    }

    @Test
    void concurrentStableBusinessEventCreatesOneAppendOnlyFact() throws Exception {
        InMemoryStore store = new InMemoryStore();
        ParticipationFacade facade = facade(store);
        var executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<RecordResultVO>> calls = new ArrayList<>();
            for (int index = 0; index < 16; index++) {
                calls.add(() -> facade.record(
                        caller("finance-service"), "tenant-1",
                        command("SUBMIT", "event-stable")));
            }

            var results = executor.invokeAll(calls).stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception error) {
                            throw new IllegalStateException(error);
                        }
                    })
                    .toList();

            assertThat(results).filteredOn(RecordResultVO::created).hasSize(1);
            assertThat(results).extracting(RecordResultVO::participationId)
                    .containsOnly("participation-1");
            assertThat(store.facts).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void servicePrincipalCannotWriteAnotherApplication() {
        ParticipationFacade facade = facade(new InMemoryStore());

        assertThatThrownBy(() -> facade.record(
                caller("inventory-service"), "tenant-1", command("SUBMIT", "event-1")))
                .isInstanceOfSatisfying(Rbac3RuleViolation.class,
                        error -> assertThat(error.reasonCode())
                                .isEqualTo("APPLICATION_BINDING_DENIED"));
    }

    private ParticipationFacade facade(InMemoryStore store) {
        return new ParticipationFacade(
                (tenantId, applicationCode, businessResource, laterAction, at) ->
                        "APPROVE".equals(laterAction)
                                ? List.of(new PriorActionRuleVO(
                                        "sod-rule-1", "SUBMIT", Instant.EPOCH))
                                : List.of(),
                store,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private BusinessParticipationCommand command(String action, String eventId) {
        return new BusinessParticipationCommand(
                "finance-service", "PAYMENT", "PAY-1", "user-1",
                action, eventId, NOW.minusSeconds(10), "trace-1");
    }

    private ServiceIdentityPrincipal caller(String applicationCode) {
        return new ServiceIdentityPrincipal(
                "service-1",
                "tenant-1",
                "service-1",
                "service-token-1",
                URI.create("https://api.example/prod/permission/rbac3"),
                12L,
                Set.of("service:participation:write",
                        "service:participation:read"),
                "finance",
                applicationCode,
                "prod",
                "credential-1",
                NOW,
                NOW.plusSeconds(300)
        );
    }

    private static final class InMemoryStore implements ParticipationRepository {
        private final Map<String, ParticipationRecordVO> byEvent =
                new HashMap<>();
        private final List<ParticipationFactVO> facts = new ArrayList<>();

        @Override
        public synchronized AppendResultVO appendAtomically(
                ParticipationRecordVO record,
                List<PriorActionRuleVO> rules) {
            ParticipationRecordVO previous = byEvent.get(record.businessEventId());
            if (previous != null) {
                if (!previous.payloadDigest().equals(record.payloadDigest())) {
                    throw new Rbac3RuleViolation("IDEMPOTENCY_CONFLICT");
                }
                return new AppendResultVO(
                        false, "participation-1", List.of());
            }
            List<String> conflicts = facts.stream()
                    .filter(fact -> fact.tenantId().equals(record.tenantId()))
                    .filter(fact -> fact.applicationCode().equals(record.applicationCode()))
                    .filter(fact -> fact.businessResource().equals(record.businessResource()))
                    .filter(fact -> fact.businessId().equals(record.businessId()))
                    .filter(fact -> fact.actorUserId().equals(record.actorUserId()))
                    .filter(fact -> rules.stream().anyMatch(rule ->
                            rule.actionCode().equals(fact.actionCode())
                                    && !fact.occurredAt().isBefore(rule.lookbackFrom())))
                    .map(ParticipationFactVO::participationId)
                    .toList();
            if (!conflicts.isEmpty()) {
                return new AppendResultVO(false, null, conflicts);
            }
            String id = "participation-" + (facts.size() + 1);
            byEvent.put(record.businessEventId(), record);
            facts.add(new ParticipationFactVO(
                    id, record.tenantId(), record.applicationCode(),
                    record.businessResource(), record.businessId(), record.actorUserId(),
                    record.actionCode(), record.businessEventId(), record.occurredAt()));
            return new AppendResultVO(true, id, List.of());
        }

        @Override
        public synchronized List<ParticipationFactVO> find(
                ConflictQueryDTO query,
                String tenantId,
                Instant lookbackFrom) {
            return List.copyOf(facts);
        }
    }
}
