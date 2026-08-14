package top.egon.cola.platform.rbac3.admin.activation;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.activation.domain.dto.RevalidationCommandDTO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.ActivationFactsVO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.CurrentActivationVO;
import top.egon.cola.platform.rbac3.admin.activation.service.ActiveRoleSetRevalidator;
import top.egon.cola.platform.rbac3.core.activation.AuthorizationRuleFacts;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ActiveRoleSetRevalidatorTest {

    @Test
    void requiresRoleReselectionWhenAnActiveRootLosesAssignmentEvidence() {
        AtomicInteger reselections = new AtomicInteger();
        var service = new ActiveRoleSetRevalidator(
                (tenantId, userId, now) -> facts(),
                (tenantId, userId, sessionId) ->
                        new CurrentActivationVO(
                                List.of("10"), 8),
                (tenantId, sessionId, expectedVersion, now, actorId) -> {
                    assertThat(expectedVersion).isEqualTo(8);
                    reselections.incrementAndGet();
                });

        var result = service.revalidate(
                new RevalidationCommandDTO(
                        "7", "9", NOW, "system"));

        assertThat(result.valid()).isFalse();
        assertThat(result.activationRequired()).isTrue();
        assertThat(result.reasonCode()).isEqualTo("ROLE_RESELECTION_REQUIRED");
        assertThat(reselections).hasValue(1);
    }

    private static final java.time.Instant NOW =
            java.time.Instant.parse("2026-07-30T00:00:00Z");

    private static ActivationFactsVO facts() {
        return new ActivationFactsVO(
                "7", "9", new RoleHierarchy(List.of(), List.of()),
                List.of(), List.of(),
                new AuthorizationRuleFacts(List.of(), List.of(), List.of(), List.of(), List.of()),
                8, 1, "directory:1", java.util.Map.of(), java.util.Map.of());
    }
}
