package top.egon.cola.component.bytecode.starter.observation;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.bytecode.api.observation.ObservationEvent;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;
import top.egon.cola.component.bytecode.bridge.MethodMetadata;
import top.egon.cola.component.bytecode.bridge.ObservationMetadata;
import top.egon.cola.component.bytecode.runtime.event.BoundedFailureStore;
import top.egon.cola.component.bytecode.runtime.observation.ObservationRuntime;
import top.egon.cola.component.bytecode.runtime.observation.ObservationState;
import top.egon.cola.component.bytecode.starter.actuator.EgonBytecodeEndpoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationPrivacyTest {

    @Test
    void runtimeAndEndpointExposeOnlyBoundedMetadataAndCounts() {
        long methodId = 92_001L;
        ClassLoader loader = Owner.class.getClassLoader();
        DispatcherRegistry.registerMethod(loader, new MethodMetadata(
                methodId, "sample/application/Owner", "work", "(Ljava/lang/String;)V",
                java.lang.reflect.Modifier.PUBLIC, false,
                Set.of(BridgeCapability.OBSERVATION)));
        DispatcherRegistry.registerObservation(loader, new ObservationMetadata(
                methodId, "APPLICATION", Map.of("channel", "test"), -1L));
        List<ObservationEvent> events = new ArrayList<>();
        ObservationRuntime runtime = new ObservationRuntime(
                true, 1.0, 10_000L, events::add, new BoundedFailureStore(4));

        ObservationState state = runtime.enter(Owner.class, methodId);
        IllegalStateException failure = new IllegalStateException(
                "password=secret token=forbidden argument-value");
        runtime.error(state, failure);
        runtime.exit(state);

        assertEquals(1, events.size());
        String eventText = events.getFirst().toString();
        assertFalse(eventText.contains("password"));
        assertFalse(eventText.contains("secret"));
        assertFalse(eventText.contains("argument-value"));
        Map<String, Object> status = new EgonBytecodeEndpoint(loader, runtime).status();
        assertEquals(1L, status.get("observationPublishedCount"));
        assertEquals(1L, status.get("observationErrorCount"));
        assertFalse(status.toString().contains("(Ljava/lang/String;)V"));
        assertFalse(status.toString().contains("sample/application/Owner"));
    }

    @Test
    void propertiesValidateRuntimeSamplingAndThresholds() {
        top.egon.cola.component.bytecode.starter.BytecodeProperties properties =
                new top.egon.cola.component.bytecode.starter.BytecodeProperties();
        properties.getObservation().setSamplingRate(0.25);
        properties.getObservation().setSlowThresholdMillis(15L);

        assertEquals(0.25, properties.getObservation().getSamplingRate());
        assertEquals(15L, properties.getObservation().getSlowThresholdMillis());
        assertTrue(properties.getObservation().isEnabled());
    }

    private static final class Owner {
    }
}
