package top.egon.cola.component.bytecode.maven.baseline;

import java.util.Set;

public record ArchitectureBaseline(int schemaVersion, Set<String> fingerprints) {

    public static final int CURRENT_SCHEMA = 1;

    public ArchitectureBaseline {
        fingerprints = fingerprints == null ? Set.of() : Set.copyOf(fingerprints);
    }

    public static ArchitectureBaseline empty() {
        return new ArchitectureBaseline(CURRENT_SCHEMA, Set.of());
    }
}
