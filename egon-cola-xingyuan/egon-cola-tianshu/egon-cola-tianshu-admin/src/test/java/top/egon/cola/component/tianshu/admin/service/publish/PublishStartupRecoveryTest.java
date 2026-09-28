package top.egon.cola.component.tianshu.admin.service.publish;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import top.egon.cola.component.tianshu.admin.config.DdcAdminProperties;
import top.egon.cola.component.tianshu.admin.model.bo.DdcPublishTaskBO;
import top.egon.cola.component.tianshu.admin.model.enums.PublishStatus;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantAdminProperties;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantBindingDTO;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantContextExecutor;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantRegistry;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublishStartupRecoveryTest {

    @Test
    void recoversEachEnabledTenantAndContinuesAfterOneTenantFails() {
        DdcPublishTaskRepository repository = mock(DdcPublishTaskRepository.class);
        DdcPendingPublishDispatcher dispatcher = mock(DdcPendingPublishDispatcher.class);
        DdcPublishTaskBO pending = task("pending", PublishStatus.PENDING);
        DdcPublishTaskBO publishing = task("publishing", PublishStatus.PUBLISHING);
        DdcPublishTaskBO unknown = task("unknown", PublishStatus.UNKNOWN);
        Instant now = Instant.parse("2026-07-25T00:00:00Z");
        DdcAdminProperties properties = new DdcAdminProperties();
        properties.getPublish().setRecoveryStaleMs(120000);
        DdcTenantAdminProperties tenantProperties = tenantProperties(
                binding(101L, "tenant-a", true), binding(202L, "tenant-b", true));
        DdcTenantRegistry registry = registry(tenantProperties);
        List<String> scanned = new CopyOnWriteArrayList<>();
        LocalDateTime staleBefore = LocalDateTime.ofInstant(now.minusMillis(120000), ZoneId.systemDefault());
        LocalDateTime claimedAt = LocalDateTime.ofInstant(now, ZoneId.systemDefault());
        when(repository.findByStatusInAndUpdatedAtBefore(List.of(
                PublishStatus.PENDING.name(), PublishStatus.PUBLISHING.name(), PublishStatus.UNKNOWN.name()), staleBefore))
                .thenAnswer(invocation -> {
                    scanned.add(MDC.get("tenantId"));
                    if ("101".equals(MDC.get("tenantId"))) throw new IllegalStateException("tenant failure");
                    return List.of(pending, publishing, unknown);
                });
        when(repository.claimStaleForRecovery("pending", List.of(
                PublishStatus.PENDING.name(), PublishStatus.PUBLISHING.name(), PublishStatus.UNKNOWN.name()),
                staleBefore, claimedAt)).thenReturn(1);
        when(repository.claimStaleForRecovery("publishing", List.of(
                PublishStatus.PENDING.name(), PublishStatus.PUBLISHING.name(), PublishStatus.UNKNOWN.name()),
                staleBefore, claimedAt)).thenReturn(0);
        when(repository.claimStaleForRecovery("unknown", List.of(
                PublishStatus.PENDING.name(), PublishStatus.PUBLISHING.name(), PublishStatus.UNKNOWN.name()),
                staleBefore, claimedAt)).thenReturn(1);
        PublishStartupRecovery recovery = new PublishStartupRecovery(repository, dispatcher, properties,
                tenantProperties, registry, new DdcTenantContextExecutor(), Clock.fixed(now, ZoneOffset.UTC));

        assertThat(recovery.recoverStale()).isEqualTo(2);

        assertThat(scanned).containsExactly("101", "202");
        verify(dispatcher).dispatch("pending");
        verify(dispatcher, never()).dispatch("publishing");
        verify(dispatcher).dispatch("unknown");
        assertThat(MDC.get("tenantId")).isNull();
        assertThat(MDC.get("userId")).isNull();
    }

    private DdcTenantAdminProperties tenantProperties(DdcTenantBindingDTO... bindings) {
        DdcTenantAdminProperties properties = new DdcTenantAdminProperties();
        properties.setBindings(List.of(bindings));
        return properties;
    }

    private DdcTenantBindingDTO binding(Long tenantId, String identity, boolean enabled) {
        return DdcTenantBindingDTO.builder().tenantId(tenantId).identityTenantId(identity).enabled(enabled).build();
    }

    private DdcTenantRegistry registry(DdcTenantAdminProperties properties) {
        DdcTenantRegistry registry = mock(DdcTenantRegistry.class);
        when(registry.requireEnabledTenant(anyLong())).thenAnswer(invocation -> properties.getBindings().stream()
                .filter(binding -> binding.getTenantId().equals(invocation.getArgument(0)))
                .findFirst().orElseThrow());
        return registry;
    }

    private DdcPublishTaskBO task(String changeId, PublishStatus status) {
        return DdcPublishTaskBO.builder().changeId(changeId).status(status.name()).ackCount(1).targetCount(2).build();
    }
}
