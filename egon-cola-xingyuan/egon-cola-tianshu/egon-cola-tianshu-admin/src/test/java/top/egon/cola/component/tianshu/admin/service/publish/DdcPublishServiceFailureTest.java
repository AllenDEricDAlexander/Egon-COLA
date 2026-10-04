package top.egon.cola.component.tianshu.admin.service.publish;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.tianshu.admin.common.DdcAdminException;
import top.egon.cola.component.tianshu.admin.config.DdcAdminProperties;
import top.egon.cola.component.tianshu.admin.model.dto.DdcPublishRequest;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigVersionRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcOperationLogRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishAckRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.tianshu.admin.service.lease.DdcConfigLeaseService;
import top.egon.cola.component.tianshu.admin.service.config.DdcYamlConfigValidator;

import java.time.Clock;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcPublishServiceFailureTest {

    @Test
    void recordsFailureThroughTheMpBoundaryWhenPreparePersistenceFails() {
        DdcConfigItemRepository configItems = mock(DdcConfigItemRepository.class);
        DdcConfigVersionRepository versions = mock(DdcConfigVersionRepository.class);
        DdcPublishTaskRepository tasks = mock(DdcPublishTaskRepository.class);
        DdcPublishAckRepository acks = mock(DdcPublishAckRepository.class);
        DdcOperationLogRepository operationLogs = mock(DdcOperationLogRepository.class);
        DdcPendingPublishDispatcher dispatcher = mock(DdcPendingPublishDispatcher.class);
        DdcConfigLeaseService leases = mock(DdcConfigLeaseService.class);
        PublishResourceLockRegistry resourceLocks = mock(PublishResourceLockRegistry.class);
        PublishCompletionWaiterRegistry waiters = mock(PublishCompletionWaiterRegistry.class);
        DdcPublishStateTransitionService transitions = mock(DdcPublishStateTransitionService.class);
        PublishFailureRecorder failureRecorder = mock(PublishFailureRecorder.class);
        DdcAdminProperties properties = new DdcAdminProperties();
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(tasks.findByChangeId("123")).thenReturn(Optional.empty());
        when(resourceLocks.tryAcquire(any(), eq("123"))).thenReturn(true);
        when(configItems.findForPublishByBizCodeAndEnvAndAppCodeAndResourceName(
                "default", "dev", "demo", "application.yml"))
                .thenThrow(new IllegalStateException("database failed"));
        DdcPublishService service = new DdcPublishService(configItems, versions, tasks, acks, operationLogs,
                dispatcher, leases, resourceLocks, waiters, transitions, failureRecorder,
                properties, new DdcYamlConfigValidator(properties.getMaxConfigBytes()),
                new TransactionTemplate(transactionManager), Clock.systemUTC());
        DdcPublishRequest request = new DdcPublishRequest();
        request.setChangeId("123");
        request.setBizCode("default");
        request.setEnv("dev");
        request.setAppCode("demo");
        request.setResourceName("application.yml");
        request.setContent("feature:\n  enabled: true\n");
        request.setFormat("YAML");
        request.setExpectedVersion(1L);

        assertThatThrownBy(() -> service.publish(request, "tester"))
                .isInstanceOf(IllegalStateException.class).hasMessage("database failed");

        verify(failureRecorder).recordFailure("123", "default", "dev", "demo", "application.yml", "database failed");
        verify(tasks, never()).insert(any());
        verify(dispatcher, never()).dispatch(anyString());
        verify(resourceLocks).release(any(), eq("123"));
    }
}
