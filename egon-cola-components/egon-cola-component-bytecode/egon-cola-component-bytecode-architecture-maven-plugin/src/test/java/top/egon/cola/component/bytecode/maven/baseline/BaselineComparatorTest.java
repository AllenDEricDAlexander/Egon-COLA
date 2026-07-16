package top.egon.cola.component.bytecode.maven.baseline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureFinding;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureLayer;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureSeverity;
import top.egon.cola.component.bytecode.api.architecture.DependencyKind;
import top.egon.cola.component.bytecode.api.architecture.LocationKind;
import top.egon.cola.component.bytecode.core.architecture.FindingFingerprint;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BaselineComparatorTest {

    @TempDir
    Path directory;

    @Test
    void separatesAcceptedNewAndStaleWithoutWritingAnything() {
        ArchitectureFinding accepted = finding("target.Accepted");
        ArchitectureFinding fresh = finding("target.New");
        Path baselinePath = directory.resolve("architecture-baseline.json");
        ArchitectureBaseline baseline = new ArchitectureBaseline(
                ArchitectureBaseline.CURRENT_SCHEMA,
                Set.of(FindingFingerprint.of(accepted), "stale"));

        BaselineComparison comparison = new BaselineComparator().compare(
                List.of(accepted, fresh), baseline);

        assertEquals(List.of(accepted), comparison.accepted());
        assertEquals(List.of(fresh), comparison.newFindings());
        assertEquals(Set.of("stale"), comparison.staleFingerprints());
        assertFalse(Files.exists(baselinePath));
    }

    private ArchitectureFinding finding(String targetClass) {
        return new ArchitectureFinding(
                "ARCH-001", ArchitectureSeverity.ERROR, "sample-domain",
                ArchitectureLayer.DOMAIN, ArchitectureLayer.APPLICATION,
                "sample.Order", "execute", "()V", targetClass,
                "run", "()V", DependencyKind.METHOD_CALL,
                LocationKind.INSTRUCTION, 10, "message", "suggestion"
        );
    }
}
