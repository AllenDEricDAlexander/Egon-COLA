package top.egon.cola.platform.rbac3.admin.activation;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationFacade;
import top.egon.cola.platform.rbac3.admin.snapshot.application.SessionSnapshotProjector;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class RoleActivationConcurrencyIT {

    @Test
    void expectedVersionAllowsOnlyOneConcurrentReplacement() throws Exception {
        var facts = RoleActivationFacadeIT.facts(
                List.of(
                        RoleActivationFacadeIT.assignment("101", "10"),
                        RoleActivationFacadeIT.assignment("102", "20")),
                List.of());
        var transaction = new RoleActivationFacadeIT.InMemoryTransaction();
        var runtime = new RoleActivationFacadeIT.RecordingRuntimeStore();
        RoleActivationFacade facade = new RoleActivationFacade(
                (tenantId, userId, now) -> facts,
                transaction,
                new SessionSnapshotProjector(),
                runtime,
                (tenantId, userId, sessionId, authVersion, sessionVersion,
                        policyVersion, now) -> new RoleActivationFacade.IssuedToken(
                        "token-" + sessionVersion, now.plusSeconds(900)),
                RoleActivationFacadeIT.policy(),
                Clock.fixed(Instant.parse("2026-07-30T12:00:00Z"), ZoneOffset.UTC));
        Callable<Boolean> first = () -> replace(facade, "10", "command-a");
        Callable<Boolean> second = () -> replace(facade, "20", "command-b");

        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Boolean> outcomes = executor.invokeAll(List.of(first, second)).stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception exception) {
                            throw new AssertionError(exception);
                        }
                    }).toList();
            assertThat(outcomes).containsExactlyInAnyOrder(true, false);
        }
    }

    private boolean replace(RoleActivationFacade facade, String roleId, String commandId) {
        try {
            facade.replace(new RoleActivationFacade.ReplaceCommand(
                    "7", "9", "99", List.of(roleId), 0, "9", commandId));
            return true;
        } catch (Rbac3RuleViolation violation) {
            assertThat(violation.reasonCode()).isEqualTo("SESSION_VERSION_CONFLICT");
            return false;
        }
    }
}
