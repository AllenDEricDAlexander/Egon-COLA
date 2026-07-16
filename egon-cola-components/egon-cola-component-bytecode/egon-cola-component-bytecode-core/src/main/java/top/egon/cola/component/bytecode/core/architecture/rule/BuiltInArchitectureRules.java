package top.egon.cola.component.bytecode.core.architecture.rule;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureRule;

import java.util.List;
import java.util.Objects;

public final class BuiltInArchitectureRules {

    private BuiltInArchitectureRules() {
    }

    public static List<ArchitectureRule> all() {
        return all(ArchitectureRuleConfiguration.defaults());
    }

    public static List<ArchitectureRule> all(ArchitectureRuleConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration");
        return List.of(
                new Arch001DomainDirectionRule(),
                new Arch002DomainTechnicalFrameworkRule(configuration),
                new Arch003ApplicationDirectionRule(),
                new Arch004ApplicationPersistenceRule(configuration),
                new Arch005FacadeContractRule(),
                new Arch006StarterBusinessRule(),
                new Arch007CommonBusinessRule(),
                new Arch008AdapterInfrastructureImplementationRule(),
                new Arch009DomainPersistenceRule(configuration),
                new Arch010FacadeImplementationRule(configuration)
        );
    }
}
