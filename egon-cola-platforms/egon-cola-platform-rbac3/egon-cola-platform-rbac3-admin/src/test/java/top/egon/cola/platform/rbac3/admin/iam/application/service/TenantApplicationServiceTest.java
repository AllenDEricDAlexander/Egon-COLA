package top.egon.cola.platform.rbac3.admin.iam.application.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.iam.application.domain.enums.TenantApplicationStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.application.domain.po.TenantApplicationPO;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-18T02:00:00Z");

    @Test
    void activeEntitlementIsEffectiveOnlyInsideItsWindow() {
        TenantApplicationPO entitlement = new TenantApplicationPO(
                1L, 7L, 9L, TenantApplicationStatusEnum.ACTIVE,
                NOW, NOW.plusSeconds(300), "MANUAL", "ticket-1",
                "initial purchase", "T-1", "admin", NOW);

        assertThat(entitlement.isEffectiveAt(NOW)).isTrue();
        assertThat(entitlement.isEffectiveAt(NOW.plusSeconds(299))).isTrue();
        assertThat(entitlement.isEffectiveAt(NOW.plusSeconds(300))).isFalse();
    }

    @Test
    void statusChangeUsesOptimisticVersion() {
        TenantApplicationPO entitlement = new TenantApplicationPO(
                1L, 7L, 9L, TenantApplicationStatusEnum.ACTIVE,
                NOW, null, "MANUAL", "ticket-1",
                null, null, "admin", NOW);

        assertThat(entitlement.changeStatus(
                TenantApplicationStatusEnum.SUSPENDED, 0L, "admin", NOW.plusSeconds(1)))
                .isTrue();
        assertThat(entitlement.getStatus())
                .isEqualTo(TenantApplicationStatusEnum.SUSPENDED);
        assertThatThrownBy(() -> entitlement.changeStatus(
                TenantApplicationStatusEnum.ACTIVE, 1L, "admin", NOW.plusSeconds(2)))
                .isInstanceOf(IllegalStateException.class);
    }
}
