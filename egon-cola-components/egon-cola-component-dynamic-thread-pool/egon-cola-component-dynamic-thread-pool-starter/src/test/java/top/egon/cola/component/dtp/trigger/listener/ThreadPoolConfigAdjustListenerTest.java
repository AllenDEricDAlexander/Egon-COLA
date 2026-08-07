package top.egon.cola.component.dtp.trigger.listener;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.common.trace.TraceKeys;
import top.egon.cola.component.common.trace.TracePropagation;
import top.egon.cola.component.common.trace.TraceScope;
import top.egon.cola.component.common.trace.TraceState;
import top.egon.cola.component.dtp.domain.IDynamicThreadPoolService;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.entity.UpdateResult;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;
import top.egon.cola.component.dtp.registry.IRegistry;
import top.egon.cola.component.dtp.registry.model.DtpAuditEvent;
import top.egon.cola.component.dtp.registry.model.DtpConfigChangeMessage;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
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
        TraceContext.clearOwnedKeys();
        dynamicThreadPoolService = mock(IDynamicThreadPoolService.class);
        registry = mock(IRegistry.class);
        listener = new ThreadPoolConfigAdjustListener(dynamicThreadPoolService, registry);
    }

    @AfterEach
    public void tearDown() {
        MDC.clear();
        TraceContext.clearOwnedKeys();
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
        AtomicReference<TraceState> listenerTrace = new AtomicReference<>();
        when(dynamicThreadPoolService.updateExecutor(message.getPayload())).thenAnswer(invocation -> {
            listenerTrace.set(TraceContext.current().orElseThrow());
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
        TraceState workerTrace = TraceState.root("worker-request");
        try (TraceScope ignored = TraceContext.open(workerTrace)) {
            listener.onMessage("test-channel", null);
            assertEquals(workerTrace, TraceContext.current().orElseThrow());
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
        TraceState workerTrace = TraceState.root("worker-request");
        MDC.put("bizKey", "worker-value");

        try (TraceScope ignored = TraceContext.open(workerTrace)) {
            listener.onMessage("test-channel", message);

            assertEquals(workerTrace, TraceContext.current().orElseThrow());
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
        TraceState producerTrace = new TraceState(
                "4bf92f3577b34da6a3ce929d0e0e4736",
                "00f067aa0ba902b7",
                null,
                "request-001",
                "01",
                "vendor=value",
                null,
                null
        );
        Map<String, String> traceContext = new LinkedHashMap<>();
        TracePropagation.inject(producerTrace, traceContext::put);
        message.setTraceContext(traceContext);
        assertEquals(producerTrace.traceparent(), message.getTraceContext().get(TraceKeys.TRACEPARENT_HEADER));
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
