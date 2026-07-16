package top.egon.cola.component.bytecode.core.architecture.rule;

import java.util.Set;

public record ArchitectureRuleConfiguration(
        Set<String> technicalFrameworkPrefixes,
        Set<String> persistencePrefixes,
        Set<String> adapterPackagePatterns,
        Set<String> technicalFrameworkAllowPrefixes
) {
    public ArchitectureRuleConfiguration(
            Set<String> technicalFrameworkPrefixes,
            Set<String> persistencePrefixes,
            Set<String> adapterPackagePatterns
    ) {
        this(technicalFrameworkPrefixes, persistencePrefixes, adapterPackagePatterns, Set.of());
    }

    public ArchitectureRuleConfiguration {
        technicalFrameworkPrefixes = Set.copyOf(technicalFrameworkPrefixes);
        persistencePrefixes = Set.copyOf(persistencePrefixes);
        adapterPackagePatterns = Set.copyOf(adapterPackagePatterns);
        technicalFrameworkAllowPrefixes = Set.copyOf(technicalFrameworkAllowPrefixes);
    }

    public static ArchitectureRuleConfiguration defaults() {
        return new ArchitectureRuleConfiguration(
                Set.of(
                        "org.springframework.",
                        "jakarta.persistence.",
                        "javax.persistence.",
                        "org.apache.ibatis.",
                        "org.mybatis.",
                        "org.hibernate.",
                        "com.baomidou."
                ),
                Set.of(
                        "jakarta.persistence.",
                        "javax.persistence.",
                        "org.apache.ibatis.",
                        "org.mybatis.",
                        "org.hibernate.",
                        "com.baomidou.",
                        "java.sql."
                ),
                Set.of("..adapter.."),
                Set.of()
        );
    }
}
