package top.egon.cola.component.bytecode.maven.rule;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureRule;
import top.egon.cola.component.bytecode.core.architecture.rule.ArchitectureRuleConfiguration;
import top.egon.cola.component.bytecode.core.architecture.rule.BuiltInArchitectureRules;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

public final class ArchitectureRuleLoader {

    public List<ArchitectureRule> load(ClassLoader classLoader) {
        return load(classLoader, ArchitectureRuleConfiguration.defaults());
    }

    public List<ArchitectureRule> load(
            ClassLoader classLoader,
            ArchitectureRuleConfiguration configuration
    ) {
        List<ArchitectureRule> rules = new ArrayList<>(BuiltInArchitectureRules.all(configuration));
        try {
            ServiceLoader.load(ArchitectureRule.class, classLoader).forEach(rules::add);
        } catch (ServiceConfigurationError error) {
            throw new IllegalArgumentException("Unable to initialize custom architecture rule", error);
        }
        validate(rules);
        return List.copyOf(rules);
    }

    private void validate(List<ArchitectureRule> rules) {
        Set<String> ids = new LinkedHashSet<>();
        for (ArchitectureRule rule : rules) {
            if (rule.id() == null || rule.id().isBlank()) {
                throw new IllegalArgumentException("Architecture rule ID must not be blank");
            }
            if (rule.severity() == null) {
                throw new IllegalArgumentException("Architecture rule severity must not be null: "
                        + rule.id());
            }
            if (!ids.add(rule.id())) {
                throw new IllegalArgumentException("Duplicate architecture rule ID: " + rule.id());
            }
        }
    }
}
