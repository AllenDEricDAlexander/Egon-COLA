package top.egon.cola.component.gateway.admin.openapi.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.gateway.admin.config.properties.GatewayAdminOpenApiProperties;
import top.egon.cola.component.gateway.admin.openapi.service.GatewayOpenApiSyncService;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thin fixed-delay trigger for OpenAPI synchronization.
 *
 * <p>中文：调度器只负责有界触发和同实例防重入；DDC、网络、快照、聚合及
 * revision CAS 全部由 {@link GatewayOpenApiSyncService} 负责，单个候选的
 * 异常不会终止后续调度。</p>
 */
@Slf4j
@Validated
@Component("gatewayOpenApiSyncReconciler")
@ConditionalOnProperty(
        name = "gateway.admin.openapi.enabled",
        havingValue = "true"
)
@ConditionalOnBean(GatewayOpenApiSyncService.class)
public class GatewayOpenApiSyncReconciler {

    private final GatewayOpenApiSyncService service;

    private final GatewayAdminOpenApiProperties properties;

    private final AtomicBoolean running = new AtomicBoolean();

    /** Creates the Spring-managed scheduled trigger. */
    @Autowired
    public GatewayOpenApiSyncReconciler(
            GatewayOpenApiSyncService service,
            GatewayAdminOpenApiProperties properties) {
        this.service = Objects.requireNonNull(service, "service");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    /**
     * Triggers one bounded synchronization pass.
     *
     * <p>The fixed-delay value is bound from the typed configuration; the
     * service applies the batch-size limit and durable CAS ownership.</p>
     */
    @Scheduled(
            fixedDelayString =
                    "${gateway.admin.openapi.reconcile-delay:PT30S}"
    )
    public void reconcile() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            properties.validate();
            service.reconcile();
        } catch (RuntimeException failure) {
            log.warn("OpenAPI synchronization tick failed safely");
        } finally {
            running.set(false);
        }
    }
}
