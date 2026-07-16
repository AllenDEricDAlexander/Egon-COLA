package top.egon.cola.component.bytecode.api.observation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObservationApiBoundaryTest {

    @Test
    void annotationTargetsOnlyMethodsAndConstructors() {
        Target target = EgonObserved.class.getAnnotation(Target.class);
        assertEquals(Set.of(ElementType.METHOD, ElementType.CONSTRUCTOR),
                Set.of(target.value()));
    }

    @Test
    void eventContainsMetadataButNoBusinessObjectsOrThrowables() {
        Set<Class<?>> componentTypes = Arrays.stream(ObservationEvent.class
                        .getRecordComponents())
                .map(RecordComponent::getType)
                .collect(Collectors.toSet());
        assertFalse(componentTypes.contains(Object.class));
        assertFalse(componentTypes.contains(Throwable.class));

        Map<String, String> tags = new java.util.LinkedHashMap<>();
        tags.put("channel", "test");
        ObservationEvent event = new ObservationEvent(
                1L, "sample.Owner", "work", "()V", "APPLICATION",
                10L, ObservationResult.SUCCESS, "NONE", "", false,
                tags, 1_000L);
        tags.put("tenant", "forbidden-late-change");
        assertEquals(Map.of("channel", "test"), event.staticTags());
        assertThrows(UnsupportedOperationException.class,
                () -> event.staticTags().put("x", "y"));
    }
}
