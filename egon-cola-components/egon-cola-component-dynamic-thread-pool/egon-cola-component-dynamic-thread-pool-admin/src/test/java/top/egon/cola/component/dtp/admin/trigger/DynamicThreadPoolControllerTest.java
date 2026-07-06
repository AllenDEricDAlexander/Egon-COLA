package top.egon.cola.component.dtp.admin.trigger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RList;
import org.redisson.api.RSet;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.CrossOrigin;
import top.egon.cola.component.dtp.admin.config.DtpTraceIdFilter;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;
import top.egon.cola.component.dtp.registry.model.DtpAuditEvent;
import top.egon.cola.component.dtp.registry.model.DtpConfigChangeMessage;
import top.egon.cola.component.dtp.registry.model.DtpRedisKeys;
import top.egon.cola.component.dtp.admin.trigger.model.ResizeExecutorRequest;
import top.egon.cola.component.dtp.admin.trigger.model.VirtualLimitRequest;
import top.egon.cola.component.dtp.admin.types.Response;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @ClassName: DynamicThreadPoolControllerTest
 * @description: 动态线程池 Controller 单元测试
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-03Month-31Day
 * @Version: 1.0
 */
@ExtendWith(MockitoExtension.class)
public class DynamicThreadPoolControllerTest {

    @Mock
    private RedissonClient mockRedissonClient;

    @Mock
    private RSet<String> mockRSet;

    @Mock
    private RKeys mockRKeys;

    @Mock
    private RBucket<ExecutorSnapshot> mockRBucket;

    @Mock
    private RList<DtpAuditEvent> mockRList;

    @Mock
    private RTopic mockRTopic;

    @InjectMocks
    private DynamicThreadPoolController controller;

    @AfterEach
    public void tearDown() {
        MDC.clear();
    }

    @Test
    public void shouldNotAllowWildcardCorsAtClassLevel() {
        CrossOrigin crossOrigin = DynamicThreadPoolController.class.getAnnotation(CrossOrigin.class);

        assertNull(crossOrigin);
    }

    @Test
    public void shouldQueryApps() {
        when(mockRedissonClient.<String>getSet(DtpRedisKeys.apps())).thenReturn(mockRSet);
        when(mockRSet.readAll()).thenReturn(Set.of("order-app"));

        Response<Set<String>> response = controller.queryApps();

        assertEquals(Response.Code.SUCCESS.getCode(), response.getCode());
        assertEquals(Set.of("order-app"), response.getData());
    }

    @Test
    public void shouldQueryInstances() {
        when(mockRedissonClient.<String>getSet(DtpRedisKeys.instances("order-app"))).thenReturn(mockRSet);
        when(mockRSet.readAll()).thenReturn(Set.of("order-8093"));

        Response<Set<String>> response = controller.queryInstances("order-app");

        assertEquals(Response.Code.SUCCESS.getCode(), response.getCode());
        assertEquals(Set.of("order-8093"), response.getData());
    }

    @Test
    public void shouldQueryExecutors() {
        ExecutorSnapshot snapshot = new ExecutorSnapshot();
        snapshot.setExecutorName("orderExecutor");
        String snapshotKey = DtpRedisKeys.snapshot("order-app", "order-8093", "orderExecutor");
        when(mockRedissonClient.getKeys()).thenReturn(mockRKeys);
        when(mockRKeys.getKeysByPattern(DtpRedisKeys.snapshot("order-app", "order-8093", "*"))).thenReturn(List.of(snapshotKey));
        when(mockRedissonClient.<ExecutorSnapshot>getBucket(snapshotKey)).thenReturn(mockRBucket);
        when(mockRBucket.get()).thenReturn(snapshot);

        Response<List<ExecutorSnapshot>> response = controller.queryExecutors("order-app", "order-8093");

        assertEquals(Response.Code.SUCCESS.getCode(), response.getCode());
        assertEquals(1, response.getData().size());
        assertSame(snapshot, response.getData().get(0));
    }

    @Test
    public void shouldQueryExecutor() {
        ExecutorSnapshot snapshot = new ExecutorSnapshot();
        snapshot.setExecutorName("orderExecutor");
        when(mockRedissonClient.<ExecutorSnapshot>getBucket(DtpRedisKeys.snapshot("order-app", "order-8093", "orderExecutor"))).thenReturn(mockRBucket);
        when(mockRBucket.get()).thenReturn(snapshot);

        Response<ExecutorSnapshot> response = controller.queryExecutor("order-app", "order-8093", "orderExecutor");

        assertEquals(Response.Code.SUCCESS.getCode(), response.getCode());
        assertSame(snapshot, response.getData());
    }

    @Test
    public void shouldPublishResizeMessage() {
        MDC.put("traceId", "trace-001");
        MDC.put("requestId", "request-001");
        ResizeExecutorRequest request = new ResizeExecutorRequest();
        request.setExecutorKind(ExecutorKind.PLATFORM_THREAD_POOL);
        request.setCorePoolSize(4);
        request.setMaximumPoolSize(16);
        request.setKeepAliveSeconds(30L);
        request.setAllowCoreThreadTimeOut(true);
        request.setOperator("admin");
        when(mockRedissonClient.getTopic(DtpRedisKeys.changeTopic("order-app"))).thenReturn(mockRTopic);

        Response<Boolean> response = controller.resizeExecutor("order-app", "order-8093", "orderExecutor", request);

        assertEquals(Response.Code.SUCCESS.getCode(), response.getCode());
        assertEquals("trace-001", response.getTraceId());
        assertTrue(response.getData());
        ArgumentCaptor<DtpConfigChangeMessage> captor = ArgumentCaptor.forClass(DtpConfigChangeMessage.class);
        verify(mockRTopic).publish(captor.capture());
        DtpConfigChangeMessage message = captor.getValue();
        assertNotNull(message.getMessageId());
        assertEquals("trace-001", message.getTraceId());
        assertEquals("request-001", message.getRequestId());
        assertEquals("order-app", message.getAppName());
        assertEquals("order-8093", message.getInstanceId());
        assertEquals("orderExecutor", message.getExecutorName());
        assertEquals(ExecutorKind.PLATFORM_THREAD_POOL, message.getExecutorKind());
        assertEquals("admin", message.getOperator());
        assertNotNull(message.getTimestamp());
        ExecutorUpdateCommand command = message.getPayload();
        assertEquals("order-app", command.getAppName());
        assertEquals("order-8093", command.getInstanceId());
        assertEquals("orderExecutor", command.getExecutorName());
        assertEquals(ExecutorKind.PLATFORM_THREAD_POOL, command.getExecutorKind());
        assertEquals(4, command.getCorePoolSize());
        assertEquals(16, command.getMaximumPoolSize());
        assertEquals(30L, command.getKeepAliveSeconds());
        assertTrue(command.getAllowCoreThreadTimeOut());
        assertNull(command.getConcurrencyLimit());
        assertEquals("trace-001", command.getTraceId());
        assertEquals("request-001", command.getRequestId());
        assertEquals("admin", command.getOperator());
    }

    @Test
    public void shouldPublishSpringTaskExecutorResizeMessage() {
        ResizeExecutorRequest request = new ResizeExecutorRequest();
        request.setExecutorKind(ExecutorKind.SPRING_THREAD_POOL_TASK_EXECUTOR);
        request.setCorePoolSize(4);
        request.setMaximumPoolSize(16);
        request.setOperator("admin");
        when(mockRedissonClient.getTopic(DtpRedisKeys.changeTopic("order-app"))).thenReturn(mockRTopic);

        Response<Boolean> response = controller.resizeExecutor("order-app", "order-8093", "taskExecutor", request);

        assertEquals(Response.Code.SUCCESS.getCode(), response.getCode());
        assertTrue(response.getData());
        ArgumentCaptor<DtpConfigChangeMessage> captor = ArgumentCaptor.forClass(DtpConfigChangeMessage.class);
        verify(mockRTopic).publish(captor.capture());
        DtpConfigChangeMessage message = captor.getValue();
        assertEquals(ExecutorKind.SPRING_THREAD_POOL_TASK_EXECUTOR, message.getExecutorKind());
        assertEquals(ExecutorKind.SPRING_THREAD_POOL_TASK_EXECUTOR, message.getPayload().getExecutorKind());
        assertEquals("taskExecutor", message.getExecutorName());
    }

    @Test
    public void shouldNotPublishInvalidResizeMessage() {
        ResizeExecutorRequest request = new ResizeExecutorRequest();
        request.setExecutorKind(ExecutorKind.PLATFORM_THREAD_POOL);
        request.setCorePoolSize(16);
        request.setMaximumPoolSize(4);

        Response<Boolean> response = controller.resizeExecutor("order-app", "order-8093", "orderExecutor", request);

        assertEquals(Response.Code.ILLEGAL_PARAMETER.getCode(), response.getCode());
        assertFalse(response.getData());
        assertTrue(response.getInfo().contains("corePoolSize"));
        verify(mockRedissonClient, never()).getTopic(anyString());
    }

    @Test
    public void shouldNotPublishResizeWhenCoreTimeoutHasInvalidKeepAlive() {
        ResizeExecutorRequest request = new ResizeExecutorRequest();
        request.setExecutorKind(ExecutorKind.PLATFORM_THREAD_POOL);
        request.setCorePoolSize(4);
        request.setMaximumPoolSize(16);
        request.setAllowCoreThreadTimeOut(true);

        Response<Boolean> response = controller.resizeExecutor("order-app", "order-8093", "orderExecutor", request);

        assertEquals(Response.Code.ILLEGAL_PARAMETER.getCode(), response.getCode());
        assertFalse(response.getData());
        assertTrue(response.getInfo().contains("keepAliveSeconds"));
        verify(mockRedissonClient, never()).getTopic(anyString());
    }

    @Test
    public void shouldNotPublishResizeWhenExecutorKindMissing() {
        ResizeExecutorRequest request = new ResizeExecutorRequest();
        request.setCorePoolSize(4);
        request.setMaximumPoolSize(16);

        Response<Boolean> response = controller.resizeExecutor("order-app", "order-8093", "orderExecutor", request);

        assertEquals(Response.Code.ILLEGAL_PARAMETER.getCode(), response.getCode());
        assertFalse(response.getData());
        assertTrue(response.getInfo().contains("executorKind"));
        verify(mockRedissonClient, never()).getTopic(anyString());
    }

    @Test
    public void shouldNotPublishResizeWhenExecutorKindIsVirtual() {
        ResizeExecutorRequest request = new ResizeExecutorRequest();
        request.setExecutorKind(ExecutorKind.VIRTUAL_THREAD_PER_TASK);
        request.setCorePoolSize(4);
        request.setMaximumPoolSize(16);

        Response<Boolean> response = controller.resizeExecutor("order-app", "order-8093", "orderExecutor", request);

        assertEquals(Response.Code.ILLEGAL_PARAMETER.getCode(), response.getCode());
        assertFalse(response.getData());
        assertTrue(response.getInfo().contains("executorKind"));
        verify(mockRedissonClient, never()).getTopic(anyString());
    }

    @Test
    public void shouldNotPublishResizeWhenExecutorKindUnsupported() {
        ResizeExecutorRequest request = new ResizeExecutorRequest();
        request.setExecutorKind(ExecutorKind.UNKNOWN);
        request.setCorePoolSize(4);
        request.setMaximumPoolSize(16);

        Response<Boolean> response = controller.resizeExecutor("order-app", "order-8093", "orderExecutor", request);

        assertEquals(Response.Code.ILLEGAL_PARAMETER.getCode(), response.getCode());
        assertFalse(response.getData());
        assertTrue(response.getInfo().contains("executorKind"));
        verify(mockRedissonClient, never()).getTopic(anyString());
    }

    @Test
    public void shouldPublishVirtualLimitMessage() {
        MDC.put("traceId", "trace-002");
        MDC.put("requestId", "request-002");
        VirtualLimitRequest request = new VirtualLimitRequest();
        request.setConcurrencyLimit(200);
        request.setOperator("ops");
        when(mockRedissonClient.getTopic(DtpRedisKeys.changeTopic("order-app"))).thenReturn(mockRTopic);

        Response<Boolean> response = controller.updateVirtualLimit("order-app", "order-8093", "virtualExecutor", request);

        assertEquals(Response.Code.SUCCESS.getCode(), response.getCode());
        assertTrue(response.getData());
        ArgumentCaptor<DtpConfigChangeMessage> captor = ArgumentCaptor.forClass(DtpConfigChangeMessage.class);
        verify(mockRTopic).publish(captor.capture());
        DtpConfigChangeMessage message = captor.getValue();
        assertEquals(ExecutorKind.VIRTUAL_THREAD_PER_TASK, message.getExecutorKind());
        assertEquals("virtualExecutor", message.getExecutorName());
        assertEquals("ops", message.getOperator());
        assertEquals(ExecutorKind.VIRTUAL_THREAD_PER_TASK, message.getPayload().getExecutorKind());
        assertEquals(200, message.getPayload().getConcurrencyLimit());
        assertNull(message.getPayload().getCorePoolSize());
        assertNull(message.getPayload().getMaximumPoolSize());
        assertEquals("trace-002", message.getPayload().getTraceId());
        assertEquals("request-002", message.getPayload().getRequestId());
    }

    @Test
    public void shouldNotPublishInvalidVirtualLimitMessage() {
        VirtualLimitRequest request = new VirtualLimitRequest();
        request.setConcurrencyLimit(0);

        Response<Boolean> response = controller.updateVirtualLimit("order-app", "order-8093", "virtualExecutor", request);

        assertEquals(Response.Code.ILLEGAL_PARAMETER.getCode(), response.getCode());
        assertFalse(response.getData());
        assertTrue(response.getInfo().contains("concurrencyLimit"));
        verify(mockRedissonClient, never()).getTopic(anyString());
    }

    @Test
    public void shouldQueryEvents() {
        DtpAuditEvent event = new DtpAuditEvent();
        event.setAppName("order-app");
        when(mockRedissonClient.<DtpAuditEvent>getList(DtpRedisKeys.event("order-app", "20260630"))).thenReturn(mockRList);
        when(mockRList.readAll()).thenReturn(List.of(event));

        Response<List<DtpAuditEvent>> response = controller.queryEvents("order-app", "20260630");

        assertEquals(Response.Code.SUCCESS.getCode(), response.getCode());
        assertEquals(1, response.getData().size());
        assertSame(event, response.getData().get(0));
    }

    @Test
    public void shouldReturnTraceIdFromMdc() {
        MDC.put("traceId", "trace-response");
        when(mockRedissonClient.<String>getSet(DtpRedisKeys.apps())).thenReturn(mockRSet);
        when(mockRSet.readAll()).thenReturn(Set.of("order-app"));

        Response<Set<String>> response = controller.queryApps();

        assertEquals("trace-response", response.getTraceId());
    }

    @Test
    public void shouldUseTraceHeadersAndClearMdc() throws ServletException, IOException {
        DtpTraceIdFilter filter = new DtpTraceIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(DtpTraceIdFilter.TRACE_ID_HEADER, "trace-header");
        request.addHeader(DtpTraceIdFilter.REQUEST_ID_HEADER, "request-header");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request, HttpServletResponse response) {
                assertEquals("trace-header", MDC.get(DtpTraceIdFilter.TRACE_ID));
                assertEquals("request-header", MDC.get(DtpTraceIdFilter.REQUEST_ID));
            }
        });

        filter.doFilter(request, response, filterChain);

        assertEquals("trace-header", response.getHeader(DtpTraceIdFilter.TRACE_ID_HEADER));
        assertNull(MDC.get(DtpTraceIdFilter.TRACE_ID));
        assertNull(MDC.get(DtpTraceIdFilter.REQUEST_ID));
    }

}
