package top.egon.cola.component.bytecode.core.architecture;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureLayer;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record LayerMapping(
        Map<ArchitectureLayer, Set<String>> explicitModules,
        Map<ArchitectureLayer, Set<String>> packagePatterns
) {
    public LayerMapping {
        explicitModules = immutableCopy(explicitModules, "explicitModules");
        packagePatterns = immutableCopy(packagePatterns, "packagePatterns");
        rejectDuplicateValues(explicitModules, "module");
        rejectDuplicateValues(packagePatterns, "package pattern");
    }

    private static Map<ArchitectureLayer, Set<String>> immutableCopy(
            Map<ArchitectureLayer, Set<String>> source,
            String name
    ) {
        Objects.requireNonNull(source, name);
        Map<ArchitectureLayer, Set<String>> copy = new EnumMap<>(ArchitectureLayer.class);
        source.forEach((layer, values) -> {
            Objects.requireNonNull(layer, "layer");
            Objects.requireNonNull(values, name + " values");
            copy.put(layer, Collections.unmodifiableSet(new LinkedHashSet<>(values)));
        });
        return Collections.unmodifiableMap(copy);
    }

    private static void rejectDuplicateValues(
            Map<ArchitectureLayer, Set<String>> mappings,
            String valueKind
    ) {
        Map<String, ArchitectureLayer> owners = new LinkedHashMap<>();
        mappings.forEach((layer, values) -> values.forEach(value -> {
            ArchitectureLayer previous = owners.putIfAbsent(value, layer);
            if (previous != null && previous != layer) {
                throw new IllegalArgumentException(
                        "Duplicate " + valueKind + " mapping '" + value
                                + "' for " + previous + " and " + layer);
            }
        }));
    }
}
