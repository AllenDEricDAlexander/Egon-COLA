package top.egon.cola.component.bytecode.starter.actuator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.bytecode.bridge.AgentBridgeStatus;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.BridgeProtocol;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;
import top.egon.cola.component.bytecode.starter.BytecodeAutoConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EgonBytecodeEndpointTest {

    @Test
    void autoConfiguresOnlyWhenEndpointIsEnabled() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        BytecodeAutoConfiguration.class,
                        BytecodeEndpointAutoConfiguration.class));

        runner.run(context -> assertEquals(1,
                context.getBeansOfType(EgonBytecodeEndpoint.class).size()));
        runner.withPropertyValues("egon.cola.component.bytecode.endpoint.enabled=false")
                .run(context -> assertEquals(0,
                        context.getBeansOfType(EgonBytecodeEndpoint.class).size()));
    }

    @Test
    void exposesBoundedStatusWithoutConfigurationPatternsOrArguments() {
        DispatcherRegistry.publishAgentStatus(new AgentBridgeStatus(
                "5.2.3",
                "ACTIVE",
                BridgeProtocol.MAJOR,
                BridgeProtocol.MINOR,
                Set.of(BridgeCapability.EXECUTOR, BridgeCapability.OBSERVATION),
                Set.of(BridgeCapability.EXECUTOR),
                3,
                4,
                1,
                List.of()
        ));
        EgonBytecodeEndpoint endpoint = new EgonBytecodeEndpoint(getClass().getClassLoader());

        Map<String, Object> status = endpoint.status();
        String text = status.toString();

        assertEquals("ACTIVE", status.get("state"));
        assertEquals(Set.of(BridgeCapability.EXECUTOR, BridgeCapability.OBSERVATION),
                status.get("requestedFeatures"));
        assertEquals(Set.of(BridgeCapability.EXECUTOR), status.get("effectiveFeatures"));
        assertFalse(text.contains("include"));
        assertFalse(text.contains("exclude"));
        assertFalse(text.contains("arguments"));
        DispatcherRegistry.publishAgentStatus(AgentBridgeStatus.disabled());
    }
}
