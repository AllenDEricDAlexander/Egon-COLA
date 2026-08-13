package top.egon.cola.platform.rbac3.admin.activation;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.activation.service.ActiveRoleSetRevalidator;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import top.egon.cola.platform.rbac3.admin.activation.domain.dto.RevalidationCommandDTO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.CurrentActivationVO;

class ActiveRoleSetRevalidatorTest {

    @Test
    void requiresRoleReselectionWhenAnActiveRootLosesAssignmentEvidence() {
        AtomicInteger reselections = new AtomicInteger();
        var service = new ActiveRoleSetRevalidator(
                (tenantId, userId, now) -> RoleActivationFacadeIT.facts(
                        List.of(), List.of()),
                (tenantId, userId, sessionId) ->
                        new CurrentActivationVO(
                                List.of("10"), 8),
                (tenantId, sessionId, expectedVersion, now, actorId) -> {
                    assertThat(expectedVersion).isEqualTo(8);
                    reselections.incrementAndGet();
                });

        var result = service.revalidate(
                new RevalidationCommandDTO(
                        "7", "9", "99", RoleActivationFacadeIT.NOW, "system"));

        assertThat(result.valid()).isFalse();
        assertThat(result.activationRequired()).isTrue();
        assertThat(result.reasonCode()).isEqualTo("ROLE_RESELECTION_REQUIRED");
        assertThat(reselections).hasValue(1);
    }
}
