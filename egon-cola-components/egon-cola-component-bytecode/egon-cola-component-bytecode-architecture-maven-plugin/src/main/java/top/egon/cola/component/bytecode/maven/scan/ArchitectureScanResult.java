package top.egon.cola.component.bytecode.maven.scan;

import top.egon.cola.component.bytecode.core.architecture.ArchitectureGraph;

public record ArchitectureScanResult(
        ArchitectureGraph graph,
        int parsedClasses,
        int cacheHits
) {
}
