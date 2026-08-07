package top.egon.cola.component.dtp.trigger.listener;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.dtp.domain.IDynamicThreadPoolService;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.entity.UpdateResult;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;
import top.egon.cola.component.dtp.registry.IRegistry;
import top.egon.cola.component.dtp.registry.model.DtpAuditEvent;
import top.egon.cola.component.dtp.registry.model.DtpConfigChangeMessage;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @ClassName: ThreadPoolConfigAdjustListenerTest
 * @description: 线程池配置监听器单元测试
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-03Month-31Day
 * @Version: 1.0
 */
public class ThreadPoolConfigAdjustListenerTest {

    private IDynamicThreadPoolService dynamicThreadPoolService;

    private IRegistry registry;

    private ThreadPoolConfigAdjustListener listener;

    @BeforeEach
    public void setUp() {
        dynamicThreadPoolService = mock(IDynamicThreadPoolService.class);
        registry = mock(IRegistry.class);
        listener = new ThreadPoolConfigAdjustListener(dynamicThreadPoolService, registry);
    }

    @AfterEach
    public void tearDown() {
        MDC.clear();
    }

    @Test
    public void test_onMessage_updateAndRecordAuditEvent() {
        DtpConfigChangeMessage message = buildMessage();
        ExecutorSnapshot before = buildSnapshot(2, 8);
        ExecutorSnapshot after = buildSnapshot(3, 9);
        UpdateResult result = new UpdateResult();
        result.setSuccess(true);
        result.setMessage("success");
        result.setBefore(before);
        result.setAfter(after);
        AtomicReference<TraceContext> listenerTrace = new AtomicReference<>();
        when(dynamicThreadPoolService.updateExecutor(message.getPayload())).thenAnswer(invocation -> {
            listenerTrace.set(TraceContext.currentOrCreate());
            return result;
        });

        listener.onMessage("test-channel", message);

        verify(dynamicThreadPoolService, times(1)).updateExecutor(message.getPayload());

        ArgumentCaptor<DtpAuditEvent> eventCaptor = ArgumentCaptor.forClass(DtpAuditEvent.class);
        verify(registry, times(1)).recordAuditEvent(eventCaptor.capture());
        DtpAuditEvent event = eventCaptor.getValue();
        assertEquals("4bf92f3577b34da6a3ce929d0e0e4736", event.getTraceId());
        assertEquals("request-001", event.getRequestId());
        assertEquals(event.getTraceId(), listenerTrace.get().traceId());
        assertEquals("00f067aa0ba902b7", listenerTrace.get().parentSpanId());
        assertEquals("01", listenerTrace.get().traceFlags());
        assertEquals("egon=sampled", listenerTrace.get().tracestate());
        assertEquals("test-app", event.getAppName());
        assertEquals("instance-001", event.getInstanceId());
        assertEquals("orderExecutor", event.getExecutorName());
        assertEquals(ExecutorKind.PLATFORM_THREAD_POOL, event.getExecutorKind());
        assertEquals("tester", event.getOperator());
        assertEquals("UPDATE", event.getOperationType());
        assertSame(before, event.getBeforeValue());
        assertSame(after, event.getAfterValue());
        assertTrue(event.isSuccess());
        assertNull(event.getErrorMessage());

        verify(registry, times(1)).reportSnapshot(after);
        assertNull(MDC.get("traceId"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    public void test_onMessage_recordFailedAuditEvent() {
        DtpConfigChangeMessage message = buildMessage();
        ExecutorSnapshot before = buildSnapshot(2, 8);
        ExecutorSnapshot after = buildSnapshot(2, 8);
        UpdateResult result = new UpdateResult();
        result.setSuccess(false);
        result.setMessage("corePoolSize must <= maximumPoolSize");
        result.setBefore(before);
        result.setAfter(after);
        when(dynamicThreadPoolService.updateExecutor(message.getPayload())).thenReturn(result);

        listener.onMessage("test-channel", message);

        ArgumentCaptor<DtpAuditEvent> eventCaptor = ArgumentCaptor.forClass(DtpAuditEvent.class);
        verify(registry, times(1)).recordAuditEvent(eventCaptor.capture());
        DtpAuditEvent event = eventCaptor.getValue();
        assertFalse(event.isSuccess());
        assertEquals("corePoolSize must <= maximumPoolSize", event.getErrorMessage());
        assertSame(before, event.getBeforeValue());
        assertSame(after, event.getAfterValue());
        verify(registry, times(1)).reportSnapshot(after);
        assertNull(MDC.get("traceId"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    public void test_onMessage_withoutAfterSnapshotDoesNotReportSnapshot() {
        DtpConfigChangeMessage message = buildMessage();
        UpdateResult result = new UpdateResult();
        result.setSuccess(false);
        result.setMessage("executor not found: orderExecutor");
        when(dynamicThreadPoolService.updateExecutor(message.getPayload())).thenReturn(result);

        listener.onMessage("test-channel", message);

        verify(dynamicThreadPoolService, times(1)).updateExecutor(message.getPayload());
        verify(registry, times(1)).recordAuditEvent(org.mockito.ArgumentMatchers.any(DtpAuditEvent.class));
        verifyNoMoreInteractions(registry);
        assertNull(MDC.get("traceId"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    public void test_onMessage_ignoresNullMessage() {
        TraceContext workerContext = TraceContext.root("worker-request");
        try (TraceContext.Scope ignored = workerContext.open()) {
            listener.onMessage("test-channel", null);
            assertEquals(workerContext.traceId(), TraceContext.getTraceId());
            assertEquals(workerContext.spanId(), MDC.get(TraceContext.SPAN_ID));
            assertEquals("worker-request", MDC.get(TraceContext.REQUEST_ID));
        }

        verifyNoMoreInteractions(dynamicThreadPoolService, registry);
        assertNull(MDC.get("traceId"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    public void test_onMessage_restoresWorkerContextWhenUpdateThrows() {
        DtpConfigChangeMessage message = buildMessage();
        when(dynamicThreadPoolService.updateExecutor(message.getPayload()))
                .thenThrow(new IllegalStateException("update failed"));
        MDC.put("bizKey", "worker-value");
        TraceContext workerContext = TraceContext.root("worker-request");

        try (TraceContext.Scope ignored = workerContext.open()) {
            listener.onMessage("test-channel", message);

            assertEquals(workerContext.traceId(), TraceContext.getTraceId());
            assertEquals(workerContext.spanId(), MDC.get(TraceContext.SPAN_ID));
            assertEquals("worker-request", MDC.get(TraceContext.REQUEST_ID));
            assertEquals("worker-value", MDC.get("bizKey"));
        }

        verify(dynamicThreadPoolService, times(1)).updateExecutor(message.getPayload());
        verifyNoMoreInteractions(registry);
        assertEquals("worker-value", MDC.get("bizKey"));
    }

    private DtpConfigChangeMessage buildMessage() {
        ExecutorUpdateCommand command = new ExecutorUpdateCommand();
        command.setAppName("test-app");
        command.setInstanceId("instance-001");
        command.setExecutorName("orderExecutor");
        command.setExecutorKind(ExecutorKind.PLATFORM_THREAD_POOL);
        command.setCorePoolSize(3);
        command.setMaximumPoolSize(9);
        command.setOperator("tester");

        DtpConfigChangeMessage message = new DtpConfigChangeMessage();
        message.setMessageId("message-001");
        message.setTraceparent("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
        message.setTracestate("egon=sampled");
        message.setRequestId("request-001");
        message.setAppName("test-app");
        message.setInstanceId("instance-001");
        message.setExecutorName("orderExecutor");
        message.setExecutorKind(ExecutorKind.PLATFORM_THREAD_POOL);
        message.setPayload(command);
        message.setOperator("tester");
        message.setTimestamp(Instant.now());
        return message;
    }

    private ExecutorSnapshot buildSnapshot(int corePoolSize, int maximumPoolSize) {
        ExecutorSnapshot snapshot = new ExecutorSnapshot();
        snapshot.setAppName("test-app");
        snapshot.setInstanceId("instance-001");
        snapshot.setExecutorName("orderExecutor");
        snapshot.setExecutorKind(ExecutorKind.PLATFORM_THREAD_POOL);
        snapshot.setCorePoolSize(corePoolSize);
        snapshot.setMaximumPoolSize(maximumPoolSize);
        return snapshot;
    }

}
