package top.egon.cola.component.ddc.admin.service.management;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishAckEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcAppEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcBizEntity;
import top.egon.cola.component.ddc.admin.model.vo.DdcConfigVO;
import top.egon.cola.component.ddc.admin.repository.DdcPublishAckRepository;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.ddc.admin.service.config.DdcConfigService;
import top.egon.cola.component.ddc.admin.service.lease.DdcInstanceAdminService;
import top.egon.cola.component.ddc.admin.service.metadata.DdcNamespaceEnvAppBindingService;
import top.egon.cola.component.ddc.admin.service.metadata.DdcAppService;
import top.egon.cola.component.ddc.admin.service.metadata.DdcBizService;
import top.egon.cola.component.ddc.admin.service.metadata.DdcScopeGate;
import top.egon.cola.component.ddc.admin.service.publish.DdcPublishService;
import top.egon.cola.component.ddc.admin.service.registry.DdcServiceRegistryService;
import top.egon.cola.component.ddc.error.management.DdcManagementErrorCode;
import top.egon.cola.component.ddc.model.management.DdcManagementConfig;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigDeleteRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementApp;
import top.egon.cola.component.ddc.model.management.DdcManagementAppQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementBiz;
import top.egon.cola.component.ddc.model.management.DdcManagementBizLookup;
import top.egon.cola.component.ddc.model.management.DdcManagementBizQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishStatus;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishTask;
import top.egon.cola.component.ddc.model.management.DdcManagementScopeQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcManagementFacadeTest {

    private DdcConfigService configService;

    private DdcPublishTaskRepository publishTaskRepository;

    private DdcPublishAckRepository publishAckRepository;

    private DdcManagementFacade facade;

    private DdcNamespaceEnvAppBindingService bindingService;

    private DdcBizService bizService;

    private DdcAppService appService;

    @BeforeEach
    void setUp() {
        configService = mock(DdcConfigService.class);
        publishTaskRepository = mock(DdcPublishTaskRepository.class);
        publishAckRepository = mock(DdcPublishAckRepository.class);
        bindingService = mock(DdcNamespaceEnvAppBindingService.class);
        bizService = mock(DdcBizService.class);
        appService = mock(DdcAppService.class);
        facade = new DdcManagementFacade(
                configService,
                mock(DdcPublishService.class),
                publishTaskRepository,
                publishAckRepository,
                mock(DdcInstanceAdminService.class),
                mock(DdcServiceRegistryService.class),
                mock(DdcScopeGate.class),
                null,
                bindingService,
                bizService,
                appService
        );
    }

    @Test
    void projectsBusinessAndApplicationCatalogWithParentStatus() {
        DdcBizEntity biz = new DdcBizEntity();
        biz.setId("business-1");
        biz.setBizCode("retail");
        biz.setBizName("Retail");
        biz.setEnabled(false);
        DdcAppEntity app = new DdcAppEntity();
        app.setId("app-1");
        app.setBizCode("retail");
        app.setAppCode("order");
        app.setAppName("Order");
        app.setEnabled(true);

        when(bizService.findById("business-1")).thenReturn(Optional.of(biz));
        when(bizService.findByBizCode("retail")).thenReturn(biz);
        when(appService.findById("app-1")).thenReturn(Optional.of(app));
        when(appService.list("retail", "order", true))
                .thenReturn(List.of(app));

        assertThat(facade.getBiz(new DdcManagementBizLookup("business-1", null)))
                .contains(new DdcManagementBiz("business-1", "retail", "Retail", false));
        assertThat(facade.getApp("app-1"))
                .contains(new DdcManagementApp(
                        "app-1", "business-1", "retail", "order", "Order",
                        true, false));
        assertThat(facade.listApps(new DdcManagementAppQuery(
                "business-1", null, "order", true)))
                .containsExactly(new DdcManagementApp(
                        "app-1", "business-1", "retail", "order", "Order",
                        true, false));
        verify(appService).list("retail", "order", true);
    }

    @Test
    void filtersBusinessCatalogByEnabledState() {
        DdcBizEntity enabled = new DdcBizEntity();
        enabled.setId("business-1");
        enabled.setBizCode("retail");
        enabled.setBizName("Retail");
        enabled.setEnabled(true);
        DdcBizEntity disabled = new DdcBizEntity();
        disabled.setId("business-2");
        disabled.setBizCode("legacy");
        disabled.setBizName("Legacy");
        disabled.setEnabled(false);
        when(bizService.list("retail", true)).thenReturn(List.of(enabled));

        assertThat(facade.listBizs(new DdcManagementBizQuery("retail", true)))
                .containsExactly(new DdcManagementBiz(
                        "business-1", "retail", "Retail", true));
    }

    @Test
    void upsertMapsStableRequestToDraftService() {
        DdcConfigVO saved = config(
                "gateway",
                "dev",
                "runtime",
                "application.yml",
                2L,
                false
        );
        when(configService.upsert(any(), eq(1L), eq("gateway-admin")))
                .thenReturn(saved);

        DdcManagementConfig response = facade.upsert(new DdcManagementConfigUpsertRequest(
                "gateway",
                "dev",
                "runtime",
                "application.yml",
                "gateway:\n  routes: []\n",
                "YAML",
                "route draft",
                1L,
                "gateway-admin"
        ));

        assertThat(response.resourceName()).isEqualTo("application.yml");
        assertThat(response.version()).isEqualTo(2L);
        verify(configService).upsert(any(), eq(1L), eq("gateway-admin"));
    }

    @Test
    void deleteMapsTheFixedYamlResourceToConfigService() {
        DdcManagementConfigDeleteRequest request =
                new DdcManagementConfigDeleteRequest(
                        "gateway",
                        "dev",
                        "runtime",
                        2L,
                        "gateway-admin",
                        "release removed"
                );

        facade.delete(request);

        verify(configService).delete(
                "gateway",
                "dev",
                "runtime",
                2L,
                "gateway-admin",
                "release removed"
        );
    }

    @Test
    void exactConfigQueryPreservesDisabledDeletedManagementState() {
        DdcConfigVO value = config(
                "gateway",
                "dev",
                "runtime",
                "application.yml",
                2L,
                true
        );
        when(configService.find(
                "gateway", "dev", "runtime"
        )).thenReturn(Optional.of(value));

        DdcManagementConfig response = facade.findConfig(new DdcManagementConfigQuery(
                "gateway", "dev", "runtime"
        ));

        assertThat(response.enabled()).isFalse();
        assertThat(response.deleted()).isTrue();
        verify(configService).find("gateway", "dev", "runtime");
    }

    @Test
    void missingExactConfigUsesStableManagementCode() {
        when(configService.find(
                "gateway", "dev", "runtime"
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facade.findConfig(new DdcManagementConfigQuery(
                "gateway", "dev", "runtime"
        )))
                .isInstanceOfSatisfying(DdcAdminException.class, exception -> {
                    assertThat(exception.getCode())
                            .isEqualTo(DdcManagementErrorCode.CONFIG_NOT_FOUND.getCode());
                    assertThat(exception.getStatus())
                            .isEqualTo(DdcManagementErrorCode.CONFIG_NOT_FOUND.getStatus());
                });
    }

    @Test
    void missingPublishTaskUsesStableManagementCode() {
        when(publishTaskRepository.findByChangeId("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> facade.getPublishTask("missing"))
                .isInstanceOfSatisfying(DdcAdminException.class, exception -> {
                    assertThat(exception.getCode())
                            .isEqualTo(DdcManagementErrorCode.PUBLISH_TASK_NOT_FOUND.getCode());
                    assertThat(exception.getStatus())
                            .isEqualTo(DdcManagementErrorCode.PUBLISH_TASK_NOT_FOUND.getStatus());
                });
    }

    @Test
    void publishTaskIncludesStableTargetAckProjection() {
        DdcPublishTaskEntity task = new DdcPublishTaskEntity();
        task.setChangeId("change-1");
        task.setStatus("SUCCESS");
        task.setTargetVersion(8L);
        task.setResourceChecksum("checksum");
        task.setTargetCount(1);
        task.setAckCount(1);
        task.setFailedCount(0);
        task.setIgnoredCount(0);
        task.setTimeoutCount(0);
        task.setAttemptCount(1);
        task.setCreatedAt(LocalDateTime.parse("2026-07-25T10:00:00"));
        task.setDispatchedAt(LocalDateTime.parse("2026-07-25T10:00:01"));
        task.setCompletedAt(LocalDateTime.parse("2026-07-25T10:00:02"));
        DdcPublishAckEntity target = new DdcPublishAckEntity();
        target.setInstanceId("engine-1");
        target.setLeaseId("lease-1");
        target.setCurrentVersion(8L);
        target.setAckStatus("SUCCESS");
        target.setAckAt(LocalDateTime.parse("2026-07-25T10:00:02"));
        when(publishTaskRepository.findByChangeId("change-1"))
                .thenReturn(Optional.of(task));
        when(publishAckRepository.findByChangeId("change-1"))
                .thenReturn(List.of(target));

        DdcManagementPublishTask response = facade.getPublishTask("change-1");

        assertThat(response.status()).isEqualTo(DdcManagementPublishStatus.SUCCESS);
        assertThat(response.targets()).singleElement().satisfies(item -> {
            assertThat(item.instanceId()).isEqualTo("engine-1");
            assertThat(item.leaseId()).isEqualTo("lease-1");
            assertThat(item.status()).isEqualTo("SUCCESS");
        });
    }

    @Test
    void scopeBindingsAreOwnedAndMappedByTheManagementFacade() {
        var query = new DdcManagementScopeQuery(
                "biz", "namespace", "test", "app");
        var binding = new top.egon.cola.component.ddc.admin.model.vo
                .DdcNamespaceEnvAppBindingVO(
                "binding-1", "biz", "namespace-id", "namespace", "test",
                "app-id", "app", "Application", true);
        when(bindingService.list("biz", "namespace", "test", "app"))
                .thenReturn(List.of(binding));

        assertThat(facade.getScopeBindings(query)).singleElement().satisfies(value -> {
            assertThat(value.bindingId()).isEqualTo("binding-1");
            assertThat(value.namespaceCode()).isEqualTo("namespace");
            assertThat(value.appCode()).isEqualTo("app");
            assertThat(value.enabled()).isTrue();
        });
        verify(bindingService).list("biz", "namespace", "test", "app");
    }

    private DdcConfigVO config(
            String appCode,
            String env,
            String namespace,
            String key,
            long version,
            boolean deleted
    ) {
        DdcConfigVO value = new DdcConfigVO();
        value.setBizCode(namespace);
        value.setAppCode(appCode);
        value.setEnv(env);
        value.setResourceName(key);
        value.setContent("gateway:\n  enabled: true\n");
        value.setFormat("YAML");
        value.setCurrentVersion(version);
        value.setEnabled(!deleted);
        value.setDeleted(deleted);
        value.setUpdatedAt(LocalDateTime.parse("2026-07-25T10:00:00"));
        return value;
    }
}
