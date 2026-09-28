package top.egon.cola.component.tianshu.admin.service.publish;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.egon.cola.component.tianshu.admin.config.DdcAdminProperties;
import top.egon.cola.component.tianshu.admin.model.bo.DdcPublishTaskBO;
import top.egon.cola.component.tianshu.admin.model.enums.PublishStatus;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantAdminProperties;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantBindingDTO;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantContextDTO;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantContextExecutor;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantRegistry;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
public class PublishTimeoutScanner {

    private static final List<String> ACTIVE_STATUSES = List.of(
            PublishStatus.PENDING.name(),
            PublishStatus.PUBLISHING.name()
    );

    private final DdcPublishTaskRepository taskRepository;
    private final DdcPublishStateTransitionService stateTransitions;
    private final DdcAdminProperties properties;
    private final DdcTenantAdminProperties tenantProperties;
    private final DdcTenantRegistry tenantRegistry;
    private final DdcTenantContextExecutor tenantContextExecutor;
    private final Clock clock;

    @Autowired
    public PublishTimeoutScanner(DdcPublishTaskRepository taskRepository,
            DdcPublishStateTransitionService stateTransitions, DdcAdminProperties properties,
            DdcTenantAdminProperties tenantProperties, DdcTenantRegistry tenantRegistry,
            DdcTenantContextExecutor tenantContextExecutor) {
        this(taskRepository, stateTransitions, properties, tenantProperties, tenantRegistry,
                tenantContextExecutor, Clock.systemUTC());
    }

    PublishTimeoutScanner(DdcPublishTaskRepository taskRepository,
            DdcPublishStateTransitionService stateTransitions, DdcAdminProperties properties,
            DdcTenantAdminProperties tenantProperties, DdcTenantRegistry tenantRegistry,
            DdcTenantContextExecutor tenantContextExecutor, Clock clock) {
        this.taskRepository = taskRepository;
        this.stateTransitions = stateTransitions;
        this.properties = properties;
        this.tenantProperties = tenantProperties;
        this.tenantRegistry = tenantRegistry;
        this.tenantContextExecutor = tenantContextExecutor;
        this.clock = clock;
    }

    @Scheduled(
            fixedDelayString = "${egon.cola.component.tianshu.admin.publish.scan-interval-ms:1000}",
            initialDelayString = "${egon.cola.component.tianshu.admin.publish.scan-interval-ms:1000}"
    )
    public int scanExpired() {
        int expired = 0;
        for (DdcTenantBindingDTO configured : tenantProperties.getBindings()) {
            if (configured == null || !configured.isEnabled()) continue;
            Long tenantId = configured.getTenantId();
            try {
                DdcTenantBindingDTO registered = tenantRegistry.requireEnabledTenant(tenantId);
                if (!registered.isEnabled()
                        || !registered.getIdentityTenantId().equals(configured.getIdentityTenantId())) {
                    throw new IllegalStateException("TENANT_BINDING_CHANGED_DURING_SCAN");
                }
                DdcTenantContextDTO context = new DdcTenantContextDTO(
                        registered.getTenantId(), registered.getIdentityTenantId(), "system:publish-timeout-scanner");
                expired += tenantContextExecutor.call(context, this::scanCurrentTenant);
            } catch (RuntimeException failure) {
                log.error("Publish timeout scan failed for tenantId={}", tenantId, failure);
            }
        }
        return expired;
    }

    private int scanCurrentTenant() {
        Instant now = clock.instant();
        return taskRepository.findByStatusIn(ACTIVE_STATUSES).stream()
                .mapToInt(task -> expire(task, now))
                .sum();
    }

    private int expire(DdcPublishTaskBO task, Instant now) {
        if (PublishStatus.PENDING.name().equals(task.getStatus())) {
            Instant deadline = instant(task.getCreatedAt())
                    .plusMillis(properties.getPublish().getDispatchTimeoutMs());
            if (!deadline.isAfter(now)) {
                stateTransitions.fail(task.getChangeId(), "DISPATCH_TIMEOUT", "publish dispatch timed out");
                return 1;
            }
            return 0;
        }
        LocalDateTime dispatchedAt = task.getDispatchedAt() == null ? task.getUpdatedAt() : task.getDispatchedAt();
        long timeoutMs = task.getTimeoutMs() == null
                ? properties.getPublish().getDefaultTimeoutMs() : task.getTimeoutMs();
        if (!instant(dispatchedAt).plusMillis(timeoutMs).isAfter(now)) {
            stateTransitions.timeout(task.getChangeId(), "publish acknowledgement timed out");
            return 1;
        }
        return 0;
    }

    private Instant instant(LocalDateTime value) {
        return value.atZone(ZoneId.systemDefault()).toInstant();
    }
}
