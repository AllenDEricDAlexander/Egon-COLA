package top.egon.cola.component.bytecode.runtime.context;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.bytecode.api.executor.ContextCarrier;
import top.egon.cola.component.bytecode.api.executor.ContextScope;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CompositeContextCarrierTest {

    @Test
    void capturesAndRestoresInOrderThenClosesInReverseOrder() {
        List<String> actions = new ArrayList<>();
        CompositeContextCarrier carrier = new CompositeContextCarrier(List.of(
                recordingCarrier("first", actions),
                recordingCarrier("second", actions)
        ));

        ContextSnapshot snapshot = carrier.capture();
        try (ContextScope ignored = snapshot.restore()) {
            actions.add("business");
        }

        assertEquals(List.of(
                "capture:first", "capture:second",
                "restore:first", "restore:second", "business",
                "close:second", "close:first"), actions);
    }

    @Test
    void partialRestoreFailureClosesAlreadyRestoredScopes() {
        List<String> actions = new ArrayList<>();
        RuntimeException failure = new RuntimeException("restore failed");
        ContextCarrier failing = new ContextCarrier() {
            @Override
            public String name() {
                return "failing";
            }

            @Override
            public Object capture() {
                actions.add("capture:failing");
                return "value";
            }

            @Override
            public ContextScope restore(Object snapshot) {
                actions.add("restore:failing");
                throw failure;
            }
        };
        ContextSnapshot snapshot = new CompositeContextCarrier(List.of(
                recordingCarrier("first", actions), failing)).capture();

        RuntimeException actual = assertThrows(RuntimeException.class, snapshot::restore);

        assertSame(failure, actual);
        assertEquals(List.of(
                "capture:first", "capture:failing", "restore:first",
                "restore:failing", "close:first"), actions);
    }

    private ContextCarrier recordingCarrier(String name, List<String> actions) {
        return new ContextCarrier() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public Object capture() {
                actions.add("capture:" + name);
                return name + "-snapshot";
            }

            @Override
            public ContextScope restore(Object snapshot) {
                actions.add("restore:" + name);
                return () -> actions.add("close:" + name);
            }
        };
    }
}
