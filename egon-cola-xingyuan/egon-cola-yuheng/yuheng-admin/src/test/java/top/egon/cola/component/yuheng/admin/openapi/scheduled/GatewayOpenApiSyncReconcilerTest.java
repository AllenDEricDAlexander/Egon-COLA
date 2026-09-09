package top.egon.cola.component.yuheng.admin.openapi.scheduled;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import top.egon.cola.component.yuheng.admin.config.properties.GatewayAdminOpenApiProperties;
import top.egon.cola.component.yuheng.admin.openapi.service.GatewayOpenApiSyncService;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GatewayOpenApiSyncReconcilerTest {

    @Test
    void scheduledEntryUsesTheBoundedConfiguredDelayAndDelegates() throws Exception {
        GatewayOpenApiSyncService service = mock(GatewayOpenApiSyncService.class);
        GatewayAdminOpenApiProperties properties =
                new GatewayAdminOpenApiProperties();
        GatewayOpenApiSyncReconciler reconciler =
                new GatewayOpenApiSyncReconciler(service, properties);

        Method method = GatewayOpenApiSyncReconciler.class.getDeclaredMethod(
                "reconcile");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertThat(scheduled.fixedDelayString())
                .isEqualTo("${gateway.admin.openapi.reconcile-delay:PT30S}");

        reconciler.reconcile();

        verify(service).reconcile();
    }
}
