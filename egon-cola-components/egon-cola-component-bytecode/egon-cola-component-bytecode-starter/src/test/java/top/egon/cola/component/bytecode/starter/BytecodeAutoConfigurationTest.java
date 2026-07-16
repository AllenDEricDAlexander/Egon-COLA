package top.egon.cola.component.bytecode.starter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.bytecode.bridge.AgentBridgeStatus;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.BridgeProtocol;
import top.egon.cola.component.bytecode.bridge.BytecodeRuntimeDispatcher;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BytecodeAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BytecodeAutoConfiguration.class));

    @AfterEach
    void resetAgentStatus() {
        DispatcherRegistry.publishAgentStatus(AgentBridgeStatus.disabled());
    }

    @Test
    void registersAndUnregistersRuntimeWhenEnabledEvenWhenAgentIsAbsent() {
        ClassLoader loader = getClass().getClassLoader();

        contextRunner.run(context -> {
            assertTrue(context.getBeansOfType(BytecodeRuntimeRegistrar.class).size() == 1);
            assertTrue(DispatcherRegistry.status(loader).registered());
            BytecodeStartupValidator.Validation validation = context
                    .getBean(BytecodeStartupValidator.class).validation();
            assertFalse(validation.agentAvailable());
            assertTrue(validation.protocolCompatible());
        });

        assertFalse(DispatcherRegistry.status(loader).registered());
    }

    @Test
    void doesNotRegisterRuntimeWhenComponentIsDisabled() {
        contextRunner.withPropertyValues("egon.cola.component.bytecode.enabled=false")
                .run(context -> assertFalse(context.containsBean("bytecodeRuntimeRegistrar")));
    }

    @Test
    void rejectsDuplicateDispatcherRegistration() {
        ClassLoader loader = getClass().getClassLoader();
        var registration = DispatcherRegistry.register(loader, "existing", new TestDispatcher());
        try {
            contextRunner.run(context -> assertNotNull(context.getStartupFailure()));
        } finally {
            registration.close();
        }
    }

    @Test
    void rejectsActiveAgentWithIncompatibleProtocol() {
        DispatcherRegistry.publishAgentStatus(new AgentBridgeStatus(
                "test",
                "ACTIVE",
                BridgeProtocol.MAJOR + 1,
                0,
                Set.of(BridgeCapability.EXECUTOR),
                Set.of(BridgeCapability.EXECUTOR),
                0,
                0,
                0,
                List.of()
        ));

        contextRunner.run(context -> assertNotNull(context.getStartupFailure()));
    }

    private static final class TestDispatcher implements BytecodeRuntimeDispatcher {

        @Override
        public int protocolMajor() {
            return BridgeProtocol.MAJOR;
        }

        @Override
        public int protocolMinor() {
            return BridgeProtocol.MINOR;
        }

        @Override
        public Set<BridgeCapability> capabilities() {
            return Set.of(BridgeCapability.EXECUTOR);
        }

        @Override
        public Runnable decorateRunnable(
                Class<?> callerClass, Executor executor, Runnable task, long callSiteId) {
            return task;
        }

        @Override
        public <V> Callable<V> decorateCallable(
                Class<?> callerClass, Executor executor, Callable<V> task, long callSiteId) {
            return task;
        }
    }
}
