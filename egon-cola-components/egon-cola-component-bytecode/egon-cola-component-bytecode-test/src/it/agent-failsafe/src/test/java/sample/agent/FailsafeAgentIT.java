package sample.agent;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.bytecode.starter.BytecodeAutoConfiguration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FailsafeAgentIT {

    @Test
    void propagatesMdcInFailsafeFork() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(BytecodeAutoConfiguration.class))
                .run(context -> {
                    MDC.put("traceId", "failsafe");
                    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                        assertEquals("failsafe", submit(executor, () -> MDC.get("traceId"))
                                .get(10, TimeUnit.SECONDS));
                    } finally {
                        MDC.clear();
                    }
                });
        System.out.println("AGENT_FAILSAFE_OK");
    }

    private <V> Future<V> submit(ExecutorService executor, java.util.concurrent.Callable<V> task) {
        return executor.submit(task);
    }
}
