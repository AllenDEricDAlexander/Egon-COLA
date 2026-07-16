package top.egon.cola.component.bytecode.maven.result;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureFinding;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureSeverity;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public record ArchitectureCheckResult(
        List<ArchitectureFinding> findings,
        Map<String, Long> countsByRule,
        Map<ArchitectureSeverity, Long> countsBySeverity
) {
    public ArchitectureCheckResult {
        findings = findings.stream().sorted(FindingOrder.COMPARATOR).toList();
        countsByRule = Collections.unmodifiableMap(new LinkedHashMap<>(new TreeMap<>(countsByRule)));
        Map<ArchitectureSeverity, Long> severityCopy = new EnumMap<>(ArchitectureSeverity.class);
        severityCopy.putAll(countsBySeverity);
        countsBySeverity = Collections.unmodifiableMap(severityCopy);
    }

    public static ArchitectureCheckResult from(List<ArchitectureFinding> findings) {
        return new ArchitectureCheckResult(
                findings,
                findings.stream().collect(Collectors.groupingBy(
                        ArchitectureFinding::ruleId, TreeMap::new, Collectors.counting())),
                findings.stream().collect(Collectors.groupingBy(
                        ArchitectureFinding::severity,
                        () -> new EnumMap<>(ArchitectureSeverity.class),
                        Collectors.counting()))
        );
    }

    public int total() {
        return findings.size();
    }
}
