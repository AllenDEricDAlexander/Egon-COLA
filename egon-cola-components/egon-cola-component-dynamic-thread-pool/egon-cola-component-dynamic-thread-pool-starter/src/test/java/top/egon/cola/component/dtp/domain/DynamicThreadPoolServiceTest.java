package top.egon.cola.component.dtp.domain;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.entity.UpdateResult;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;
import top.egon.cola.component.dtp.executor.ManagedExecutor;
import top.egon.cola.component.dtp.executor.ManagedExecutorRegistry;
import top.egon.cola.component.dtp.executor.adapter.ThreadPoolExecutorManagedExecutor;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @ClassName: DynamicThreadPoolServiceTest
 * @description: 动态线程池服务单元测试
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-03Month-31Day
 * @Version: 1.0
 */
public class DynamicThreadPoolServiceTest {

    @Test
    public void test_queryExecutorSnapshots() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 8,
                30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );
        ManagedExecutor managedExecutor = new ThreadPoolExecutorManagedExecutor(
                "app", "instance", "orderExecutor", executor
        );
        DynamicThreadPoolService service = new DynamicThreadPoolService(
                new ManagedExecutorRegistry(List.of(managedExecutor))
        );

        List<ExecutorSnapshot> snapshots = service.queryExecutorSnapshots();

        assertEquals(1, snapshots.size());
        assertEquals("orderExecutor", snapshots.get(0).getExecutorName());
        assertEquals("app", snapshots.get(0).getAppName());
        assertEquals("instance", snapshots.get(0).getInstanceId());
    }

    @Test
    public void test_queryExecutorSnapshot() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 8,
                30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );
        ManagedExecutor managedExecutor = new ThreadPoolExecutorManagedExecutor(
                "app", "instance", "orderExecutor", executor
        );
        DynamicThreadPoolService service = new DynamicThreadPoolService(
                new ManagedExecutorRegistry(List.of(managedExecutor))
        );

        ExecutorSnapshot snapshot = service.queryExecutorSnapshot("orderExecutor");

        assertNotNull(snapshot);
        assertEquals("orderExecutor", snapshot.getExecutorName());
        assertNull(service.queryExecutorSnapshot("missingExecutor"));
    }

    @Test
    public void test_updateExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 8,
                30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );
        ManagedExecutor managedExecutor = new ThreadPoolExecutorManagedExecutor(
                "app", "instance", "orderExecutor", executor
        );
        DynamicThreadPoolService service = new DynamicThreadPoolService(
                new ManagedExecutorRegistry(List.of(managedExecutor))
        );
        ExecutorUpdateCommand command = new ExecutorUpdateCommand();
        command.setAppName("app");
        command.setInstanceId("instance");
        command.setExecutorName("orderExecutor");
        command.setExecutorKind(ExecutorKind.PLATFORM_THREAD_POOL);
        command.setCorePoolSize(3);
        command.setMaximumPoolSize(9);

        UpdateResult result = service.updateExecutor(command);

        assertTrue(result.isSuccess());
        assertEquals("success", result.getMessage());
        assertEquals(3, executor.getCorePoolSize());
        assertEquals(9, executor.getMaximumPoolSize());
        assertEquals(2, result.getBefore().getCorePoolSize());
        assertEquals(3, result.getAfter().getCorePoolSize());
    }

    @Test
    public void test_updateExecutorRejectsBlankName() {
        DynamicThreadPoolService service = new DynamicThreadPoolService(
                new ManagedExecutorRegistry(List.of())
        );

        UpdateResult result = service.updateExecutor(new ExecutorUpdateCommand());

        assertFalse(result.isSuccess());
        assertEquals("executorName must not be blank", result.getMessage());
    }

    @Test
    public void test_updateExecutorRejectsMissingExecutor() {
        DynamicThreadPoolService service = new DynamicThreadPoolService(
                new ManagedExecutorRegistry(List.of())
        );
        ExecutorUpdateCommand command = new ExecutorUpdateCommand();
        command.setExecutorName("missingExecutor");

        UpdateResult result = service.updateExecutor(command);

        assertFalse(result.isSuccess());
        assertEquals("executor not found: missingExecutor", result.getMessage());
    }

    @Test
    public void test_updateExecutorRejectsMissingOrBlankAppName() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 8,
                30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );
        ManagedExecutor managedExecutor = new ThreadPoolExecutorManagedExecutor(
                "app", "instance", "orderExecutor", executor
        );
        DynamicThreadPoolService service = new DynamicThreadPoolService(
                new ManagedExecutorRegistry(List.of(managedExecutor))
        );
        ExecutorUpdateCommand missingAppCommand = new ExecutorUpdateCommand();
        missingAppCommand.setInstanceId("instance");
        missingAppCommand.setExecutorName("orderExecutor");
        missingAppCommand.setExecutorKind(ExecutorKind.PLATFORM_THREAD_POOL);
        missingAppCommand.setCorePoolSize(3);
        missingAppCommand.setMaximumPoolSize(9);
        ExecutorUpdateCommand blankAppCommand = new ExecutorUpdateCommand();
        blankAppCommand.setAppName(" ");
        blankAppCommand.setInstanceId("instance");
        blankAppCommand.setExecutorName("orderExecutor");
        blankAppCommand.setExecutorKind(ExecutorKind.PLATFORM_THREAD_POOL);
        blankAppCommand.setCorePoolSize(3);
        blankAppCommand.setMaximumPoolSize(9);

        UpdateResult missingAppResult = service.updateExecutor(missingAppCommand);
        UpdateResult blankAppResult = service.updateExecutor(blankAppCommand);

        assertFalse(missingAppResult.isSuccess());
        assertEquals("appName must not be blank", missingAppResult.getMessage());
        assertFalse(blankAppResult.isSuccess());
        assertEquals("appName must not be blank", blankAppResult.getMessage());
        assertEquals(2, executor.getCorePoolSize());
        assertEquals(8, executor.getMaximumPoolSize());
    }

    @Test
    public void test_updateExecutorRejectsMissingOrBlankInstanceId() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 8,
                30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );
        ManagedExecutor managedExecutor = new ThreadPoolExecutorManagedExecutor(
                "app", "instance", "orderExecutor", executor
        );
        DynamicThreadPoolService service = new DynamicThreadPoolService(
                new ManagedExecutorRegistry(List.of(managedExecutor))
        );
        ExecutorUpdateCommand missingInstanceCommand = new ExecutorUpdateCommand();
        missingInstanceCommand.setAppName("app");
        missingInstanceCommand.setExecutorName("orderExecutor");
        missingInstanceCommand.setExecutorKind(ExecutorKind.PLATFORM_THREAD_POOL);
        missingInstanceCommand.setCorePoolSize(3);
        missingInstanceCommand.setMaximumPoolSize(9);
        ExecutorUpdateCommand blankInstanceCommand = new ExecutorUpdateCommand();
        blankInstanceCommand.setAppName("app");
        blankInstanceCommand.setInstanceId(" ");
        blankInstanceCommand.setExecutorName("orderExecutor");
        blankInstanceCommand.setExecutorKind(ExecutorKind.PLATFORM_THREAD_POOL);
        blankInstanceCommand.setCorePoolSize(3);
        blankInstanceCommand.setMaximumPoolSize(9);

        UpdateResult missingInstanceResult = service.updateExecutor(missingInstanceCommand);
        UpdateResult blankInstanceResult = service.updateExecutor(blankInstanceCommand);

        assertFalse(missingInstanceResult.isSuccess());
        assertEquals("instanceId must not be blank", missingInstanceResult.getMessage());
        assertFalse(blankInstanceResult.isSuccess());
        assertEquals("instanceId must not be blank", blankInstanceResult.getMessage());
        assertEquals(2, executor.getCorePoolSize());
        assertEquals(8, executor.getMaximumPoolSize());
    }

    @Test
    public void test_updateExecutorRejectsNullExecutorKind() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 8,
                30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );
        ManagedExecutor managedExecutor = new ThreadPoolExecutorManagedExecutor(
                "app", "instance", "orderExecutor", executor
        );
        DynamicThreadPoolService service = new DynamicThreadPoolService(
                new ManagedExecutorRegistry(List.of(managedExecutor))
        );
        ExecutorUpdateCommand command = new ExecutorUpdateCommand();
        command.setAppName("app");
        command.setInstanceId("instance");
        command.setExecutorName("orderExecutor");
        command.setCorePoolSize(3);
        command.setMaximumPoolSize(9);

        UpdateResult result = service.updateExecutor(command);

        assertFalse(result.isSuccess());
        assertEquals("executorKind must not be null", result.getMessage());
        assertEquals(2, executor.getCorePoolSize());
        assertEquals(8, executor.getMaximumPoolSize());
    }

    @Test
    public void test_updateExecutorRejectsMismatchedInstanceId() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 8,
                30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );
        ManagedExecutor managedExecutor = new ThreadPoolExecutorManagedExecutor(
                "app", "instance", "orderExecutor", executor
        );
        DynamicThreadPoolService service = new DynamicThreadPoolService(
                new ManagedExecutorRegistry(List.of(managedExecutor))
        );
        ExecutorUpdateCommand command = new ExecutorUpdateCommand();
        command.setAppName("app");
        command.setInstanceId("another-instance");
        command.setExecutorName("orderExecutor");
        command.setExecutorKind(ExecutorKind.PLATFORM_THREAD_POOL);
        command.setCorePoolSize(3);
        command.setMaximumPoolSize(9);

        UpdateResult result = service.updateExecutor(command);

        assertFalse(result.isSuccess());
        assertEquals("instanceId mismatch: another-instance", result.getMessage());
        assertEquals(2, executor.getCorePoolSize());
        assertEquals(8, executor.getMaximumPoolSize());
    }

    @Test
    public void test_updateExecutorRejectsMismatchedAppNameAndKind() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 8,
                30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );
        ManagedExecutor managedExecutor = new ThreadPoolExecutorManagedExecutor(
                "app", "instance", "orderExecutor", executor
        );
        DynamicThreadPoolService service = new DynamicThreadPoolService(
                new ManagedExecutorRegistry(List.of(managedExecutor))
        );
        ExecutorUpdateCommand wrongAppCommand = new ExecutorUpdateCommand();
        wrongAppCommand.setAppName("another-app");
        wrongAppCommand.setInstanceId("instance");
        wrongAppCommand.setExecutorName("orderExecutor");
        wrongAppCommand.setExecutorKind(ExecutorKind.PLATFORM_THREAD_POOL);
        wrongAppCommand.setCorePoolSize(3);
        wrongAppCommand.setMaximumPoolSize(9);
        ExecutorUpdateCommand wrongKindCommand = new ExecutorUpdateCommand();
        wrongKindCommand.setAppName("app");
        wrongKindCommand.setInstanceId("instance");
        wrongKindCommand.setExecutorName("orderExecutor");
        wrongKindCommand.setExecutorKind(ExecutorKind.VIRTUAL_THREAD_PER_TASK);
        wrongKindCommand.setCorePoolSize(3);
        wrongKindCommand.setMaximumPoolSize(9);

        UpdateResult wrongAppResult = service.updateExecutor(wrongAppCommand);
        UpdateResult wrongKindResult = service.updateExecutor(wrongKindCommand);

        assertFalse(wrongAppResult.isSuccess());
        assertEquals("appName mismatch: another-app", wrongAppResult.getMessage());
        assertFalse(wrongKindResult.isSuccess());
        assertEquals("executorKind mismatch: VIRTUAL_THREAD_PER_TASK", wrongKindResult.getMessage());
        assertEquals(2, executor.getCorePoolSize());
        assertEquals(8, executor.getMaximumPoolSize());
    }

}
