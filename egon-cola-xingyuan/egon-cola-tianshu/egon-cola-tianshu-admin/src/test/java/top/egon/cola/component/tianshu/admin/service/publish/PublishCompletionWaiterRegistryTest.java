package top.egon.cola.component.tianshu.admin.service.publish;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class PublishCompletionWaiterRegistryTest {

    @BeforeEach
    void bindTenant() {
        MDC.put("tenantId", "101");
    }

    @AfterEach
    void clearTenant() {
        MDC.clear();
    }

    @Test
    void sameChangeIdUsesIndependentWaitersAcrossTenants() {
        PublishCompletionWaiterRegistry registry = new PublishCompletionWaiterRegistry();
        PublishCompletionWaiterRegistry.PublishWaiter tenantA = registry.register("change-1");
        assertThat(registry.register("change-1")).isSameAs(tenantA);

        MDC.put("tenantId", "202");
        PublishCompletionWaiterRegistry.PublishWaiter tenantB = registry.register("change-1");
        assertThat(tenantB).isNotSameAs(tenantA);

        MDC.put("tenantId", "101");
        registry.signal("change-1");
        assertThat(tenantA.signalVersion()).isEqualTo(1L);
        assertThat(tenantB.signalVersion()).isZero();
        registry.remove("change-1", tenantA);

        MDC.put("tenantId", "202");
        registry.signal("change-1");
        assertThat(tenantB.signalVersion()).isEqualTo(1L);
    }
}
