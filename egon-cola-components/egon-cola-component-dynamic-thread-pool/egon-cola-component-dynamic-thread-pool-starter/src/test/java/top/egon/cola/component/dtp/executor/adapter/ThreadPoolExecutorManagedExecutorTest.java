package top.egon.cola.component.dtp.executor.adapter;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.entity.UpdateResult;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @ClassName: ThreadPoolExecutorManagedExecutorTest
 * @description: 平台线程池托管执行器测试
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-06Month-29Day
 * @Version: 1.0
 */
public class ThreadPoolExecutorManagedExecutorTest {

    @Test
    public void test_snapshotFields() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                5, 10,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );
        ThreadPoolExecutorManagedExecutor managedExecutor = new ThreadPoolExecutorManagedExecutor(
                "test-app", "instance-01", "testExecutor", executor
        );

        ExecutorSnapshot snapshot = managedExecutor.snapshot();

        assertEquals("test-app", snapshot.getAppName());
        assertEquals("instance-01", snapshot.getInstanceId());
        assertEquals("testExecutor", snapshot.getExecutorName());
        assertEquals(ExecutorKind.PLATFORM_THREAD_POOL, snapshot.getExecutorKind());
        assertFalse(snapshot.isVirtual());
        assertTrue(snapshot.isResizable());
        assertEquals(5, snapshot.getCorePoolSize());
        assertEquals(10, snapshot.getMaximumPoolSize());
        assertEquals(0, snapshot.getActiveCount());
        assertEquals(0, snapshot.getPoolSize());
        assertEquals(0L, snapshot.getTaskCount());
        assertEquals(0L, snapshot.getCompletedTaskCount());
        assertEquals("LinkedBlockingQueue", snapshot.getQueueType());
        assertEquals(0, snapshot.getQueueSize());
        assertEquals(100, snapshot.getRemainingCapacity());
        assertNotNull(snapshot.getReportTime());
        assertTrue(managedExecutor.supportsResize());
        assertFalse(managedExecutor.supportsVirtualThread());
        assertTrue(managedExecutor.supportsQueueMetrics());
    }

    @Test
    public void test_updateSuccess() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                5, 10,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );
        ThreadPoolExecutorManagedExecutor managedExecutor = new ThreadPoolExecutorManagedExecutor(
                "test-app", "instance-01", "testExecutor", executor
        );
        ExecutorUpdateCommand command = new ExecutorUpdateCommand();
        command.setCorePoolSize(12);
        command.setMaximumPoolSize(15);
        command.setKeepAliveSeconds(30L);
        command.setAllowCoreThreadTimeOut(true);

        UpdateResult result = managedExecutor.update(command);

        assertTrue(result.isSuccess());
        assertEquals("success", result.getMessage());
        assertNotNull(result.getBefore());
        assertNotNull(result.getAfter());
        assertEquals(5, result.getBefore().getCorePoolSize());
        assertEquals(10, result.getBefore().getMaximumPoolSize());
        assertEquals(12, result.getAfter().getCorePoolSize());
        assertEquals(15, result.getAfter().getMaximumPoolSize());
        assertEquals(12, executor.getCorePoolSize());
        assertEquals(15, executor.getMaximumPoolSize());
        assertEquals(30L, executor.getKeepAliveTime(TimeUnit.SECONDS));
        assertTrue(executor.allowsCoreThreadTimeOut());
    }

    @Test
    public void test_updateRejectsIncompleteResizeCommand() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                5, 10,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );
        ThreadPoolExecutorManagedExecutor managedExecutor = new ThreadPoolExecutorManagedExecutor(
                "test-app", "instance-01", "testExecutor", executor
        );
        ExecutorUpdateCommand command = new ExecutorUpdateCommand();
        command.setCorePoolSize(12);

        UpdateResult result = managedExecutor.update(command);

        assertFalse(result.isSuccess());
        assertEquals("corePoolSize and maximumPoolSize must be provided together", result.getMessage());
        assertEquals(5, executor.getCorePoolSize());
        assertEquals(10, executor.getMaximumPoolSize());
        assertEquals(5, result.getAfter().getCorePoolSize());
        assertEquals(10, result.getAfter().getMaximumPoolSize());
    }

    @Test
    public void test_updateRejectsNegativeKeepAliveBeforeResize() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                5, 10,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );
        ThreadPoolExecutorManagedExecutor managedExecutor = new ThreadPoolExecutorManagedExecutor(
                "test-app", "instance-01", "testExecutor", executor
        );
        ExecutorUpdateCommand command = new ExecutorUpdateCommand();
        command.setCorePoolSize(12);
        command.setMaximumPoolSize(15);
        command.setKeepAliveSeconds(-1L);

        UpdateResult result = managedExecutor.update(command);

        assertFalse(result.isSuccess());
        assertEquals("keepAliveSeconds must be >= 0", result.getMessage());
        assertEquals(5, executor.getCorePoolSize());
        assertEquals(10, executor.getMaximumPoolSize());
        assertEquals(60L, executor.getKeepAliveTime(TimeUnit.SECONDS));
    }

    @Test
    public void test_updateRejectsCoreThreadTimeoutWithoutKeepAliveBeforeResize() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                5, 10,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );
        ThreadPoolExecutorManagedExecutor managedExecutor = new ThreadPoolExecutorManagedExecutor(
                "test-app", "instance-01", "testExecutor", executor
        );
        ExecutorUpdateCommand command = new ExecutorUpdateCommand();
        command.setCorePoolSize(12);
        command.setMaximumPoolSize(15);
        command.setKeepAliveSeconds(0L);
        command.setAllowCoreThreadTimeOut(true);

        UpdateResult result = managedExecutor.update(command);

        assertFalse(result.isSuccess());
        assertEquals("keepAliveSeconds must be > 0 when allowCoreThreadTimeOut is true", result.getMessage());
        assertEquals(5, executor.getCorePoolSize());
        assertEquals(10, executor.getMaximumPoolSize());
        assertEquals(60L, executor.getKeepAliveTime(TimeUnit.SECONDS));
        assertFalse(executor.allowsCoreThreadTimeOut());
    }

    @Test
    public void test_updateRejectsZeroKeepAliveWhenCoreTimeoutAlreadyEnabledBeforeResize() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                5, 10,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );
        executor.allowCoreThreadTimeOut(true);
        ThreadPoolExecutorManagedExecutor managedExecutor = new ThreadPoolExecutorManagedExecutor(
                "test-app", "instance-01", "testExecutor", executor
        );
        ExecutorUpdateCommand command = new ExecutorUpdateCommand();
        command.setCorePoolSize(12);
        command.setMaximumPoolSize(15);
        command.setKeepAliveSeconds(0L);

        UpdateResult result = managedExecutor.update(command);

        assertFalse(result.isSuccess());
        assertEquals("keepAliveSeconds must be > 0 when allowCoreThreadTimeOut is true", result.getMessage());
        assertEquals(5, executor.getCorePoolSize());
        assertEquals(10, executor.getMaximumPoolSize());
        assertEquals(60L, executor.getKeepAliveTime(TimeUnit.SECONDS));
        assertTrue(executor.allowsCoreThreadTimeOut());
    }

    @Test
    public void test_updateDisablesCoreThreadTimeoutBeforeSettingZeroKeepAlive() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                5, 10,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );
        executor.allowCoreThreadTimeOut(true);
        ThreadPoolExecutorManagedExecutor managedExecutor = new ThreadPoolExecutorManagedExecutor(
                "test-app", "instance-01", "testExecutor", executor
        );
        ExecutorUpdateCommand command = new ExecutorUpdateCommand();
        command.setKeepAliveSeconds(0L);
        command.setAllowCoreThreadTimeOut(false);

        UpdateResult result = managedExecutor.update(command);

        assertTrue(result.isSuccess());
        assertEquals("success", result.getMessage());
        assertEquals(0L, executor.getKeepAliveTime(TimeUnit.SECONDS));
        assertFalse(executor.allowsCoreThreadTimeOut());
    }

}
