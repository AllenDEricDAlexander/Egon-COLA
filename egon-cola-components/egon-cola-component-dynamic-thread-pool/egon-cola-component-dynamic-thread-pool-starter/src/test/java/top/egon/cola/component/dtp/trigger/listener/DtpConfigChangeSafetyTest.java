package top.egon.cola.component.dtp.trigger.listener;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.dtp.domain.IDynamicThreadPoolService;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.entity.UpdateResult;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;
import top.egon.cola.component.dtp.registry.IRegistry;
import top.egon.cola.component.dtp.registry.model.DtpConfigChangeMessage;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DtpConfigChangeSafetyTest {
    private DtpConfigChangeMessage message() {
        ExecutorUpdateCommand command = new ExecutorUpdateCommand();
        command.setAppName("app"); command.setInstanceId("one"); command.setExecutorName("pool");
        command.setExecutorKind(ExecutorKind.PLATFORM_THREAD_POOL); command.setCorePoolSize(1); command.setMaximumPoolSize(2);
        DtpConfigChangeMessage message = new DtpConfigChangeMessage();
        message.setAppName("app"); message.setInstanceId("one"); message.setExecutorName("pool");
        message.setExecutorKind(ExecutorKind.PLATFORM_THREAD_POOL); message.setPayload(command); message.setMessageId("command-1");
        return message;
    }

    @Test
    void ignoresOtherInstancesAndRejectsMismatchedEnvelopesBeforeApplying() {
        var service = mock(IDynamicThreadPoolService.class); var registry = mock(IRegistry.class);
        var listener = new ThreadPoolConfigAdjustListener(service, registry, "app", "one");
        var message = message(); message.setInstanceId("two"); listener.onMessage("topic", message);
        message.setInstanceId("one"); message.getPayload().setExecutorName("another"); listener.onMessage("topic", message);
        verifyNoInteractions(service, registry);
    }

    @Test
    void concurrentDuplicatesAndSameIdConflictsApplyOnlyOnce() throws Exception {
        var service = mock(IDynamicThreadPoolService.class); var registry = mock(IRegistry.class);
        var listener = new ThreadPoolConfigAdjustListener(service, registry, "app", "one");
        UpdateResult result = new UpdateResult(); result.setSuccess(true);
        when(service.updateExecutor(any())).thenReturn(result);
        var message = message();
        try (var workers = Executors.newFixedThreadPool(6)) {
            Callable<Void> call = () -> { listener.onMessage("topic", message); return null; };
            for (var future : workers.invokeAll(Collections.nCopies(24, call))) { future.get(2, TimeUnit.SECONDS); }
        }
        message.getPayload().setCorePoolSize(2);
        listener.onMessage("topic", message);
        verify(service, times(1)).updateExecutor(any());
        verify(registry, times(1)).recordAuditEvent(any());
    }

    @Test
    void auditFailureDoesNotPreventReportingTheActualAppliedSnapshotOrReapplyDuplicate() {
        var service = mock(IDynamicThreadPoolService.class); var registry = mock(IRegistry.class);
        var listener = new ThreadPoolConfigAdjustListener(service, registry, "app", "one");
        UpdateResult result = new UpdateResult(); result.setSuccess(true); result.setAfter(new ExecutorSnapshot());
        when(service.updateExecutor(any())).thenReturn(result);
        doThrow(new IllegalStateException("audit offline")).when(registry).recordAuditEvent(any());
        var message = message(); listener.onMessage("topic", message); listener.onMessage("topic", message);
        verify(service, times(1)).updateExecutor(any());
        verify(registry).reportSnapshot(same(result.getAfter()));
    }
}
