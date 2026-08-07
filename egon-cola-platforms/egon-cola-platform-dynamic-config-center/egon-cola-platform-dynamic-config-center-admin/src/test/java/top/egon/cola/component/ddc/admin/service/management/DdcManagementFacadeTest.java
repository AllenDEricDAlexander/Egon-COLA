package top.egon.cola.component.ddc.admin.service.management;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishAckEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.ddc.admin.model.vo.DdcConfigVO;
import top.egon.cola.component.ddc.admin.repository.DdcPublishAckRepository;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.ddc.admin.service.config.DdcConfigService;
import top.egon.cola.component.ddc.admin.service.lease.DdcInstanceAdminService;
import top.egon.cola.component.ddc.admin.service.metadata.DdcScopeGate;
import top.egon.cola.component.ddc.admin.service.publish.DdcPublishService;
import top.egon.cola.component.ddc.admin.service.registry.DdcServiceRegistryService;
import top.egon.cola.component.ddc.management.client.DdcManagementErrorCode;
import top.egon.cola.component.ddc.management.model.DdcManagementConfig;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigDeleteRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishStatus;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishTask;

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

    @BeforeEach
    void setUp() {
        configService = mock(DdcConfigService.class);
        publishTaskRepository = mock(DdcPublishTaskRepository.class);
        publishAckRepository = mock(DdcPublishAckRepository.class);
        facade = new DdcManagementFacade(
                configService,
                mock(DdcPublishService.class),
                publishTaskRepository,
                publishAckRepository,
                mock(DdcInstanceAdminService.class),
                mock(DdcServiceRegistryService.class),
                mock(DdcScopeGate.class)
        );
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
                "gateway:\n  routes: []\n",
                "route draft",
                1L,
                "gateway-admin"
        ));

        assertThat(response.configKey()).isEqualTo("application.yml");
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
        task.setContentChecksum("checksum");
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
        value.setConfigKey(key);
        value.setConfigValue("gateway:\n  enabled: true\n");
        value.setValueType("YAML");
        value.setCurrentVersion(version);
        value.setEnabled(!deleted);
        value.setDeleted(deleted);
        value.setUpdatedAt(LocalDateTime.parse("2026-07-25T10:00:00"));
        return value;
    }
}
