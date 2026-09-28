package top.egon.cola.component.tianshu.admin.service.publish;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.egon.cola.component.tianshu.admin.common.DdcAdminException;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PublishStartupRecovery implements ApplicationRunner {

    private static final List<String> ACTIVE_STATUSES = List.of(
            PublishStatus.PENDING.name(), PublishStatus.PUBLISHING.name(), PublishStatus.UNKNOWN.name());

    private final DdcPublishTaskRepository taskRepository;
    private final DdcPendingPublishDispatcher dispatcher;
    private final DdcAdminProperties properties;
    private final DdcTenantAdminProperties tenantProperties;
    private final DdcTenantRegistry tenantRegistry;
    private final DdcTenantContextExecutor tenantContextExecutor;
    private final Clock clock;

    @Autowired
    public PublishStartupRecovery(DdcPublishTaskRepository taskRepository,
            DdcPendingPublishDispatcher dispatcher, DdcAdminProperties properties,
            DdcTenantAdminProperties tenantProperties, DdcTenantRegistry tenantRegistry,
            DdcTenantContextExecutor tenantContextExecutor) {
        this(taskRepository, dispatcher, properties, tenantProperties, tenantRegistry,
                tenantContextExecutor, Clock.systemUTC());
    }

    PublishStartupRecovery(DdcPublishTaskRepository taskRepository, DdcPendingPublishDispatcher dispatcher,
            DdcAdminProperties properties, DdcTenantAdminProperties tenantProperties,
            DdcTenantRegistry tenantRegistry, DdcTenantContextExecutor tenantContextExecutor, Clock clock) {
        this.taskRepository = taskRepository;
        this.dispatcher = dispatcher;
        this.properties = properties;
        this.tenantProperties = tenantProperties;
        this.tenantRegistry = tenantRegistry;
        this.tenantContextExecutor = tenantContextExecutor;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        recoverStale();
    }

    @Scheduled(
            fixedDelayString = "${egon.cola.component.tianshu.admin.publish.recovery-stale-ms:120000}",
            initialDelayString = "${egon.cola.component.tianshu.admin.publish.recovery-stale-ms:120000}"
    )
    public int recoverStale() {
        int recovered = 0;
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
                        registered.getTenantId(), registered.getIdentityTenantId(), "system:publish-startup-recovery");
                recovered += tenantContextExecutor.call(context, this::recoverCurrentTenant);
            } catch (RuntimeException failure) {
                log.error("Publish startup recovery failed for tenantId={}", tenantId, failure);
            }
        }
        return recovered;
    }

    private int recoverCurrentTenant() {
        long staleMs = properties.getPublish().getRecoveryStaleMs();
        if (staleMs <= 0) {
            throw new IllegalStateException("Tianshu publish recovery stale interval must be positive");
        }
        LocalDateTime staleBefore = LocalDateTime.ofInstant(
                clock.instant().minusMillis(staleMs), ZoneId.systemDefault());
        LocalDateTime claimedAt = LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault());
        List<DdcPublishTaskBO> tasks = taskRepository.findByStatusInAndUpdatedAtBefore(ACTIVE_STATUSES, staleBefore);
        return tasks.stream().mapToInt(task -> replay(task.getChangeId(), staleBefore, claimedAt)).sum();
    }

    private int replay(String changeId, LocalDateTime staleBefore, LocalDateTime claimedAt) {
        int claimed = taskRepository.claimStaleForRecovery(changeId, ACTIVE_STATUSES, staleBefore, claimedAt);
        if (claimed != 1) return 0;
        try {
            dispatcher.dispatch(changeId);
        } catch (DdcAdminException ignored) {
            // The dispatcher persisted the deterministic rejection on the task.
        }
        return 1;
    }
}
