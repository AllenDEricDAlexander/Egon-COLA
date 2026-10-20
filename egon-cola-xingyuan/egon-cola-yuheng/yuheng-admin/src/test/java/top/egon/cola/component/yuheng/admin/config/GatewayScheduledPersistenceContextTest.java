package top.egon.cola.component.yuheng.admin.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import top.egon.cola.component.yuheng.admin.observability.controller.scheduled.GatewayObservabilityRetentionReaper;
import top.egon.cola.component.yuheng.admin.observability.service.GatewayCallEventIngestService;
import top.egon.cola.component.yuheng.admin.reporting.controller.scheduled.GatewayHmacNonceReaper;
import top.egon.cola.component.yuheng.admin.reporting.repository.GatewayHmacNonceRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static top.egon.cola.component.yuheng.admin.config.GatewayPersistenceTestSupport.persistenceContext;

class GatewayScheduledPersistenceContextTest {

    @AfterEach
    void clearContext() {
        MDC.clear();
    }

    @Test
    void nonceCleanupUsesDeploymentIdentityAndRestoresTheSchedulerThread() throws Exception {
        GatewayHmacNonceRepository nonces = mock(GatewayHmacNonceRepository.class);
        doAnswer(invocation -> {
            assertThat(MDC.get("tenantId")).isEqualTo("7001");
            assertThat(MDC.get("userId")).isEqualTo("scheduled-test-service");
            return null;
        }).when(nonces).deleteExpired(any());
        MDC.put("tenantId", "previous-tenant");
        MDC.put("userId", "previous-user");

        new GatewayHmacNonceReaper(nonces, persistenceContext()).reap();

        verify(nonces).deleteExpired(any());
        assertThat(MDC.get("tenantId")).isEqualTo("previous-tenant");
        assertThat(MDC.get("userId")).isEqualTo("previous-user");
    }

    @Test
    void retentionFailurePropagatesAndLeavesNoTenantOnTheSchedulerThread() {
        GatewayCallEventIngestService service = mock(GatewayCallEventIngestService.class);
        IllegalStateException failure = new IllegalStateException("retention unavailable");
        when(service.purgeExpired()).thenAnswer(invocation -> {
            assertThat(MDC.get("tenantId")).isEqualTo("7001");
            assertThat(MDC.get("userId")).isEqualTo("scheduled-test-service");
            throw failure;
        });

        assertThatThrownBy(() -> new GatewayObservabilityRetentionReaper(service, persistenceContext()).reap())
                .isSameAs(failure);

        assertThat(MDC.get("tenantId")).isNull();
        assertThat(MDC.get("userId")).isNull();
    }
}
