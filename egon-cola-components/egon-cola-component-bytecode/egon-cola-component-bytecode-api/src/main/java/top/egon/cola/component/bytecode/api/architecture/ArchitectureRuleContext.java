package top.egon.cola.component.bytecode.api.architecture;

import java.util.Collection;
import java.util.Optional;

public interface ArchitectureRuleContext {

    Collection<ArchitectureType> types();

    Collection<ArchitectureDependency> dependencies();

    Optional<ArchitectureType> findType(String className);

    ArchitectureFinding finding(
            ArchitectureRule rule,
            ArchitectureDependency dependency,
            String message,
            String suggestion
    );
}
