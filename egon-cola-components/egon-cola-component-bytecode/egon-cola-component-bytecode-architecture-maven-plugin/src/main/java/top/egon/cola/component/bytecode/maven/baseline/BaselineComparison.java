package top.egon.cola.component.bytecode.maven.baseline;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureFinding;

import java.util.List;
import java.util.Set;

public record BaselineComparison(
        List<ArchitectureFinding> accepted,
        List<ArchitectureFinding> newFindings,
        Set<String> staleFingerprints
) {
    public BaselineComparison {
        accepted = List.copyOf(accepted);
        newFindings = List.copyOf(newFindings);
        staleFingerprints = Set.copyOf(staleFingerprints);
    }
}
