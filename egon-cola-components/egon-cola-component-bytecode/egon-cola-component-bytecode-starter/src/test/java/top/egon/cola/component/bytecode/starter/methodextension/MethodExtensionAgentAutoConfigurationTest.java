package top.egon.cola.component.bytecode.starter.methodextension;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.FilteredClassLoader;
import top.egon.cola.component.bytecode.bridge.AgentBridgeStatus;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.BridgeProtocol;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;
import top.egon.cola.component.bytecode.starter.BytecodeAutoConfiguration;
import top.egon.cola.component.methodextension.aop.MethodExtensionAop;
import top.egon.cola.component.methodextension.autoconfigure.MethodExtensionAutoConfiguration;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodExtensionAgentAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    MethodExtensionAutoConfiguration.class,
                    MethodExtensionAgentAutoConfiguration.class,
                    BytecodeAutoConfiguration.class
            ));

    @AfterEach
    void resetAgentStatus() {
        DispatcherRegistry.publishAgentStatus(AgentBridgeStatus.disabled());
    }

    @Test
    void keepsAopAsDefaultWithoutRegisteringAgentAdapter() {
        contextRunner.run(context -> {
            assertTrue(context.containsBean("methodExtensionAop"));
            assertFalse(context.containsBean("methodExtensionRuntimeAdapter"));
            assertFalse(DispatcherRegistry.status(getClass().getClassLoader())
                    .capabilities().contains(BridgeCapability.METHOD_EXTENSION));
        });
    }

    @Test
    void bytecodeStarterRemainsUsableWithoutOptionalMethodExtensionClasses() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        MethodExtensionAgentAutoConfiguration.class,
                        BytecodeAutoConfiguration.class
                ))
                .withClassLoader(new FilteredClassLoader(
                        "top.egon.cola.component.methodextension"))
                .run(context -> {
                    assertFalse(context.containsBean("methodExtensionRuntimeAdapter"));
                    assertTrue(context.getStartupFailure() == null);
                });
    }

    @Test
    void registersOnlyAgentEngineWhenCompatibleAgentIsActive() {
        publishAgent(BridgeProtocol.MAJOR, Set.of(BridgeCapability.METHOD_EXTENSION));

        contextRunner.withPropertyValues(
                        "egon.cola.component.method-extension.engine=agent")
                .run(context -> {
                    assertFalse(context.containsBean("methodExtensionAop"));
                    assertTrue(context.containsBean("methodExtensionRuntimeAdapter"));
                    assertTrue(DispatcherRegistry.status(getClass().getClassLoader())
                            .capabilities().contains(BridgeCapability.METHOD_EXTENSION));
                });
    }

    @Test
    void disabledModeRegistersNeitherEngine() {
        contextRunner.withPropertyValues(
                        "egon.cola.component.method-extension.enabled=false",
                        "egon.cola.component.method-extension.engine=agent")
                .run(context -> {
                    assertFalse(context.containsBean("methodExtensionAop"));
                    assertFalse(context.containsBean("methodExtensionRuntimeAdapter"));
                });
    }

    @Test
    void agentModeFailsWithoutAttachedAgentOrEffectiveCapability() {
        contextRunner.withPropertyValues(
                        "egon.cola.component.method-extension.engine=agent")
                .run(context -> assertNotNull(context.getStartupFailure()));

        publishAgent(BridgeProtocol.MAJOR, Set.of(BridgeCapability.EXECUTOR));
        contextRunner.withPropertyValues(
                        "egon.cola.component.method-extension.engine=agent")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    void agentModeFailsOnProtocolMismatch() {
        publishAgent(
                BridgeProtocol.MAJOR + 1,
                Set.of(BridgeCapability.METHOD_EXTENSION)
        );

        contextRunner.withPropertyValues(
                        "egon.cola.component.method-extension.engine=agent")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }

    private void publishAgent(int protocolMajor, Set<BridgeCapability> features) {
        DispatcherRegistry.publishAgentStatus(new AgentBridgeStatus(
                "test",
                "ACTIVE",
                protocolMajor,
                BridgeProtocol.MINOR,
                features,
                features,
                1,
                0,
                0,
                List.of()
        ));
    }
}
