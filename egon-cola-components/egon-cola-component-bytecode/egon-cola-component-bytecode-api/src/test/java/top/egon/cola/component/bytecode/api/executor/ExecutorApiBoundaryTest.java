package top.egon.cola.component.bytecode.api.executor;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutorApiBoundaryTest {

    @Test
    void carrierContractCapturesAndRestoresOpaqueState() {
        assertArrayEquals(new String[]{"capture", "name", "restore"},
                Arrays.stream(ContextCarrier.class.getDeclaredMethods())
                        .map(Method::getName)
                        .sorted()
                        .toArray(String[]::new));
        assertTrue(AutoCloseable.class.isAssignableFrom(ContextScope.class));
    }

    @Test
    void eventContainsOnlyBoundedScalarMetadata() {
        Set<String> components = Arrays.stream(ExecutorEvent.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                "callSiteId", "executorName", "executorType", "phase", "result",
                "exceptionGroup", "virtualThread", "submittedNanos", "startedNanos",
                "completedNanos"), components);
        assertTrue(Arrays.stream(ExecutorEvent.class.getRecordComponents())
                .map(RecordComponent::getType)
                .allMatch(type -> type.isPrimitive() || type == String.class));
    }
}
