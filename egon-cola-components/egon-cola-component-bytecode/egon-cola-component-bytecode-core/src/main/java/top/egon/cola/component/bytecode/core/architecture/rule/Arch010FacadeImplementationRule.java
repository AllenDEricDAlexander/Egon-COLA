package top.egon.cola.component.bytecode.core.architecture.rule;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureFinding;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureLayer;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureRule;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureRuleContext;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureSeverity;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureType;
import top.egon.cola.component.bytecode.api.architecture.LocationKind;

import java.util.ArrayList;
import java.util.List;

public final class Arch010FacadeImplementationRule implements ArchitectureRule {

    private final ArchitectureRuleConfiguration configuration;

    public Arch010FacadeImplementationRule(ArchitectureRuleConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String id() {
        return "ARCH-010";
    }

    @Override
    public ArchitectureSeverity severity() {
        return ArchitectureSeverity.ERROR;
    }

    @Override
    public List<ArchitectureFinding> evaluate(ArchitectureRuleContext context) {
        List<ArchitectureFinding> findings = new ArrayList<>();
        for (ArchitectureType type : context.types()) {
            if (type.layer() == ArchitectureLayer.FACADE
                    && !type.interfaceType()
                    && !matchesAdapterPackage(type.className())) {
                findings.add(new ArchitectureFinding(
                        id(),
                        severity(),
                        type.module(),
                        type.layer(),
                        ArchitectureLayer.ADAPTER,
                        type.className(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        LocationKind.CLASS,
                        null,
                        "Facade implementations must reside in a configured Adapter package.",
                        "Move the implementation to an Adapter package and retain only the interface in Facade."
                ));
            }
        }
        return List.copyOf(findings);
    }

    private boolean matchesAdapterPackage(String className) {
        String paddedClassName = "." + className + ".";
        return configuration.adapterPackagePatterns().stream().anyMatch(pattern -> {
            if (pattern.startsWith("..") && pattern.endsWith("..") && pattern.length() > 4) {
                String segment = pattern.substring(2, pattern.length() - 2);
                return paddedClassName.contains("." + segment + ".");
            }
            if (pattern.endsWith("..")) {
                String prefix = pattern.substring(0, pattern.length() - 2);
                return className.startsWith(prefix + ".");
            }
            return className.startsWith(pattern);
        });
    }
}
