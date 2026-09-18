package top.egon.cola.component.common.core.cache;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EgonColaCachePortContractTest {

    @Test
    void exposesThreeJdkOnlyMethods() throws Exception {
        assertEquals(Object.class, EgonColaCachePort.class
                .getMethod("get", String.class, String.class, Supplier.class).getReturnType());
        assertEquals(List.class, EgonColaCachePort.class
                .getMethod("getAll", String.class, List.class, Function.class).getReturnType());
        assertEquals(void.class, EgonColaCachePort.class
                .getMethod("registerEvictionAfterCommit", String.class, Collection.class, Collection.class)
                .getReturnType());
    }

    @Test
    void portSourceImportsJdkOnly() throws Exception {
        Path portSource = Path.of("src/main/java/top/egon/cola/component/common/core/cache/EgonColaCachePort.java");
        assertTrue(Files.exists(portSource), "port source must live at " + portSource);
        List<String> nonJdkImports;
        try (var lines = Files.lines(portSource)) {
            nonJdkImports = lines
                    .filter(line -> line.startsWith("import "))
                    .filter(line -> !line.startsWith("import java.util.")
                            && !line.startsWith("import java.util.function."))
                    .toList();
        }
        assertEquals(List.of(), nonJdkImports);
    }

    @Test
    void recordingStubRoundTripsArguments() {
        RecordingPort port = new RecordingPort();

        Object value = new Object();
        Object got = port.get("UserBO", "41:7", () -> value);

        assertSame(value, got);
        assertEquals(List.of("get:UserBO:41:7"), port.calls);

        List<Object> all = port.getAll("UserBO", List.of("41:7", "41:8"), id -> "v-" + id);
        assertEquals(List.of("v-41:7", "v-41:8"), all);

        port.registerEvictionAfterCommit("UserBO", List.of("41:7"), List.of("41:*"));
        assertEquals(
                List.of("get:UserBO:41:7",
                        "getAll:UserBO:[41:7, 41:8]",
                        "evict:UserBO:[41:7]:[41:*]"),
                port.calls);
        assertEquals(3, port.calls.size());
    }

    private static final class RecordingPort implements EgonColaCachePort {

        private final List<String> calls = Collections.synchronizedList(new ArrayList<>());

        @Override
        public Object get(String cacheName, String key, Supplier<Object> loader) {
            calls.add("get:" + cacheName + ":" + key);
            return loader.get();
        }

        @Override
        public List<Object> getAll(String cacheName, List<String> keys, Function<String, Object> loader) {
            calls.add("getAll:" + cacheName + ":" + keys);
            return keys.stream().map(loader).toList();
        }

        @Override
        public void registerEvictionAfterCommit(String cacheName, Collection<String> exactKeys,
                                                Collection<String> globPatterns) {
            calls.add("evict:" + cacheName + ":" + exactKeys + ":" + globPatterns);
        }
    }
}
