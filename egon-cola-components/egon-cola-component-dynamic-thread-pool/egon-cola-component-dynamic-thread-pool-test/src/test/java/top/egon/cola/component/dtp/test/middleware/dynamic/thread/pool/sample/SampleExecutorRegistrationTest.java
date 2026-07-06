package top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.sample;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.dtp.test.config.ThreadPoolConfig;
import top.egon.cola.component.dtp.domain.DynamicThreadPoolService;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;
import top.egon.cola.component.dtp.executor.ManagedExecutor;
import top.egon.cola.component.dtp.executor.ManagedExecutorRegistry;
import top.egon.cola.component.dtp.executor.adapter.BoundedVirtualThreadManagedExecutor;
import top.egon.cola.component.dtp.executor.adapter.ThreadPoolExecutorManagedExecutor;
import top.egon.cola.component.dtp.executor.virtual.BoundedVirtualThreadExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @description 示例应用执行器注册轻量测试
 */
public class SampleExecutorRegistrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ThreadPoolConfig.class);

    @Test
    public void test_virtualExecutorShouldRegisterWithoutRedis() {
        contextRunner.run(context -> {
            List<ManagedExecutor> managedExecutors = new ArrayList<>();
            context.getBeansOfType(ThreadPoolExecutor.class).forEach((executorName, executor) ->
                    managedExecutors.add(new ThreadPoolExecutorManagedExecutor("sample-app", "sample-instance", executorName, executor)));
            context.getBeansOfType(BoundedVirtualThreadExecutor.class).forEach((executorName, executor) ->
                    managedExecutors.add(new BoundedVirtualThreadManagedExecutor("sample-app", "sample-instance", executorName, executor)));
            DynamicThreadPoolService service = new DynamicThreadPoolService(new ManagedExecutorRegistry(managedExecutors));

            List<ExecutorSnapshot> snapshots = service.queryExecutorSnapshots();

            assertThat(snapshots).anySatisfy(snapshot -> {
                assertThat(snapshot.getExecutorName()).isEqualTo("virtualTaskExecutor");
                assertThat(snapshot.getExecutorKind()).isEqualTo(ExecutorKind.VIRTUAL_THREAD_PER_TASK);
                assertThat(snapshot.isVirtual()).isTrue();
                assertThat(snapshot.getConcurrencyLimit()).isEqualTo(100);
            });
            assertThat(snapshots).anySatisfy(snapshot -> {
                assertThat(snapshot.getExecutorName()).isEqualTo("threadPoolExecutor01");
                assertThat(snapshot.getExecutorKind()).isEqualTo(ExecutorKind.PLATFORM_THREAD_POOL);
                assertThat(snapshot.getCorePoolSize()).isPositive();
                assertThat(snapshot.getMaximumPoolSize()).isGreaterThanOrEqualTo(snapshot.getCorePoolSize());
            });
        });
    }

}
