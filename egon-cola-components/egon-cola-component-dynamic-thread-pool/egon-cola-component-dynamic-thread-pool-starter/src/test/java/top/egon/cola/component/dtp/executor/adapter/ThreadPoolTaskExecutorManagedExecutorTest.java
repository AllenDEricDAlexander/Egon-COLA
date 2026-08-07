package top.egon.cola.component.dtp.executor.adapter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.dtp.executor.ManagedExecutor;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @description Spring 平台线程池托管执行器测试
 */
public class ThreadPoolTaskExecutorManagedExecutorTest {

    private static final String BUSINESS_ID = "businessId";

    @AfterEach
    public void clearMdc() {
        MDC.clear();
    }

    @Test
    public void test_submitShouldPropagateContextAndPreserveTaskDecorator() throws Exception {
        AtomicBoolean decoratorInvoked = new AtomicBoolean();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(10);
        executor.setTaskDecorator(task -> () -> {
            decoratorInvoked.set(true);
            task.run();
        });
        executor.initialize();
        ManagedExecutor managedExecutor = new ThreadPoolTaskExecutorManagedExecutor(
                "test-app", "instance-01", "taskExecutor", executor
        );
        try {
            TraceContext expected = TraceContext.root("spring-request");
            Future<TaskContext> future;
            try (TraceContext.Scope ignored = expected.open()) {
                MDC.put(BUSINESS_ID, "spring-business");
                future = managedExecutor.submit(() -> new TaskContext(
                        TraceContext.capture(),
                        MDC.get(BUSINESS_ID),
                        Thread.currentThread().isVirtual()
                ));
            }

            TaskContext actual = future.get(3, TimeUnit.SECONDS);

            assertEquals(expected.traceId(), actual.traceContext().traceId());
            assertEquals(expected.spanId(), actual.traceContext().spanId());
            assertEquals(expected.requestId(), actual.traceContext().requestId());
            assertEquals("spring-business", actual.businessId());
            assertFalse(actual.virtualThread());
            assertTrue(decoratorInvoked.get());
            assertNull(executor.submit(TraceContext::getTraceId).get(3, TimeUnit.SECONDS));
            assertNull(executor.submit(() -> MDC.get(BUSINESS_ID)).get(3, TimeUnit.SECONDS));
        } finally {
            managedExecutor.shutdownNow();
        }
    }

    private record TaskContext(TraceContext traceContext,
                               String businessId,
                               boolean virtualThread) {
    }

}
