package top.egon.cola.component.bytecode.maven.baseline;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureFinding;
import top.egon.cola.component.bytecode.core.architecture.FindingFingerprint;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class BaselineComparator {

    public BaselineComparison compare(
            List<ArchitectureFinding> findings,
            ArchitectureBaseline baseline
    ) {
        List<ArchitectureFinding> accepted = new ArrayList<>();
        List<ArchitectureFinding> newFindings = new ArrayList<>();
        Set<String> current = new LinkedHashSet<>();
        for (ArchitectureFinding finding : findings) {
            String fingerprint = FindingFingerprint.of(finding);
            current.add(fingerprint);
            if (baseline.fingerprints().contains(fingerprint)) {
                accepted.add(finding);
            } else {
                newFindings.add(finding);
            }
        }
        Set<String> stale = new LinkedHashSet<>(baseline.fingerprints());
        stale.removeAll(current);
        return new BaselineComparison(accepted, newFindings, stale);
    }
}
