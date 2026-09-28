package top.egon.cola.component.tianshu.admin.service.publish;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcPublishTimeoutScannerTest {

    @Test
    void springSelectsTheProductionConstructor() {
        DdcTenantAdminProperties tenantProperties = tenantProperties(
                binding(101L, "tenant-a", true));
        DdcTenantRegistry tenantRegistry = registry(tenantProperties);
        new ApplicationContextRunner()
                .withBean(DdcPublishTaskRepository.class, () -> mock(DdcPublishTaskRepository.class))
                .withBean(DdcPublishStateTransitionService.class, () -> mock(DdcPublishStateTransitionService.class))
                .withBean(DdcAdminProperties.class)
                .withBean(DdcTenantAdminProperties.class, () -> tenantProperties)
                .withBean(DdcTenantRegistry.class, () -> tenantRegistry)
                .withBean(DdcTenantContextExecutor.class)
                .withBean(PublishTimeoutScanner.class)
                .run(context -> assertThat(context).hasSingleBean(PublishTimeoutScanner.class));
    }

    @Test
    void scansEachEnabledTenantAndContinuesAfterOneTenantFails() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-24T10:00:00Z"), ZoneOffset.UTC);
        DdcPublishTaskRepository repository = mock(DdcPublishTaskRepository.class);
        DdcPublishStateTransitionService transitions = mock(DdcPublishStateTransitionService.class);
        DdcAdminProperties properties = new DdcAdminProperties();
        properties.getPublish().setDispatchTimeoutMs(5000);
        DdcTenantAdminProperties tenantProperties = tenantProperties(
                binding(101L, "tenant-a", true), binding(202L, "tenant-b", true),
                binding(303L, "tenant-disabled", false));
        DdcTenantRegistry registry = registry(tenantProperties);
        List<String> observedTenants = new CopyOnWriteArrayList<>();
        when(repository.findByStatusIn(List.of(PublishStatus.PENDING.name(), PublishStatus.PUBLISHING.name())))
                .thenAnswer(invocation -> {
                    String tenantId = MDC.get("tenantId");
                    observedTenants.add(tenantId);
                    if ("101".equals(tenantId)) throw new IllegalStateException("tenant failure");
                    LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault());
                    return List.of(task("pending", PublishStatus.PENDING, now.minusSeconds(6), null, 30000L));
                });
        PublishTimeoutScanner scanner = new PublishTimeoutScanner(repository, transitions, properties,
                tenantProperties, registry, new DdcTenantContextExecutor(), clock);

        assertThat(scanner.scanExpired()).isEqualTo(1);

        assertThat(observedTenants).containsExactly("101", "202");
        verify(registry, org.mockito.Mockito.times(2)).requireEnabledTenant(anyLong());
        verify(transitions).fail("pending", "DISPATCH_TIMEOUT", "publish dispatch timed out");
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

    private DdcPublishTaskBO task(String changeId, PublishStatus status, LocalDateTime createdAt,
            LocalDateTime dispatchedAt, Long timeoutMs) {
        return DdcPublishTaskBO.builder().changeId(changeId).status(status.name())
                .createdAt(createdAt).dispatchedAt(dispatchedAt).timeoutMs(timeoutMs).build();
    }
}
