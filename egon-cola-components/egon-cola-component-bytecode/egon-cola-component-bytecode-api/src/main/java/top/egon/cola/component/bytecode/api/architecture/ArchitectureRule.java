package top.egon.cola.component.bytecode.api.architecture;

import java.util.List;

public interface ArchitectureRule {

    String id();

    ArchitectureSeverity severity();

    List<ArchitectureFinding> evaluate(ArchitectureRuleContext context);
}
