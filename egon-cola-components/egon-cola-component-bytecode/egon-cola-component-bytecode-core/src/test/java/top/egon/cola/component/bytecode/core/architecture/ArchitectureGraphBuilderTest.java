package top.egon.cola.component.bytecode.core.architecture;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureDependency;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureFinding;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureLayer;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureRule;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureSeverity;
import top.egon.cola.component.bytecode.api.architecture.DependencyKind;
import top.egon.cola.component.bytecode.api.architecture.LocationKind;
import top.egon.cola.component.bytecode.core.classfile.ClassDependency;
import top.egon.cola.component.bytecode.core.classfile.ClassMetadata;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchitectureGraphBuilderTest {

    @Test
    void buildsLayeredGraphAndDeduplicatesOnlyIdenticalProvenance() {
        ClassDependency first = dependency(20);
        ClassDependency duplicate = dependency(20);
        ClassDependency otherLine = dependency(21);
        ClassMetadata metadata = new ClassMetadata(
                "sample-domain",
                "sample.domain.Order",
                "java.lang.Object",
                Set.of(),
                Set.of("sample.Marker"),
                false,
                List.of(first, duplicate, otherLine)
        );

        ArchitectureGraph graph = new ArchitectureGraphBuilder().build(
                List.of(metadata),
                (module, className) -> className.contains(".domain.")
                        ? ArchitectureLayer.DOMAIN : ArchitectureLayer.INFRASTRUCTURE
        );

        assertEquals(1, graph.types().size());
        assertEquals(ArchitectureLayer.DOMAIN, graph.types().getFirst().layer());
        assertEquals(2, graph.dependencies().size());
        assertEquals(ArchitectureLayer.INFRASTRUCTURE,
                graph.dependencies().getFirst().targetLayer());
    }

    @Test
    void contextCopiesDependencyProvenanceIntoFinding() {
        ArchitectureGraph graph = new ArchitectureGraphBuilder().build(
                List.of(new ClassMetadata("sample-domain", "sample.domain.Order",
                        "java.lang.Object", Set.of(), Set.of(), false,
                        List.of(dependency(20)))),
                (module, className) -> className.contains(".domain.")
                        ? ArchitectureLayer.DOMAIN : ArchitectureLayer.INFRASTRUCTURE
        );
        DefaultArchitectureRuleContext context = new DefaultArchitectureRuleContext(graph);
        ArchitectureRule rule = new ArchitectureRule() {
            @Override
            public String id() {
                return "ARCH-TEST";
            }

            @Override
            public ArchitectureSeverity severity() {
                return ArchitectureSeverity.ERROR;
            }

            @Override
            public List<ArchitectureFinding> evaluate(
                    top.egon.cola.component.bytecode.api.architecture.ArchitectureRuleContext context) {
                return List.of();
            }
        };

        ArchitectureDependency dependency = graph.dependencies().getFirst();
        ArchitectureFinding finding = context.finding(rule, dependency, "message", "suggestion");

        assertEquals("ARCH-TEST", finding.ruleId());
        assertEquals(20, finding.lineNumber());
        assertEquals("sample.domain.Order", finding.sourceClass());
        assertEquals("sample.infrastructure.Store", finding.targetClass());
    }

    private ClassDependency dependency(int lineNumber) {
        return new ClassDependency(
                "sample.domain.Order",
                "save",
                "()V",
                "sample.infrastructure.Store",
                "persist",
                "()V",
                DependencyKind.METHOD_CALL,
                LocationKind.INSTRUCTION,
                lineNumber
        );
    }
}
