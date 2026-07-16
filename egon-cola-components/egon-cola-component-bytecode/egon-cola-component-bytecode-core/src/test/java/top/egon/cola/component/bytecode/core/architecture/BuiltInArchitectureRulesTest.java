package top.egon.cola.component.bytecode.core.architecture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureDependency;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureFinding;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureLayer;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureRule;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureType;
import top.egon.cola.component.bytecode.api.architecture.DependencyKind;
import top.egon.cola.component.bytecode.api.architecture.LocationKind;
import top.egon.cola.component.bytecode.core.architecture.rule.BuiltInArchitectureRules;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuiltInArchitectureRulesTest {

    @Test
    void registryContainsExactlyTheTenApprovedRulesInOrder() {
        assertEquals(
                List.of("ARCH-001", "ARCH-002", "ARCH-003", "ARCH-004", "ARCH-005",
                        "ARCH-006", "ARCH-007", "ARCH-008", "ARCH-009", "ARCH-010"),
                BuiltInArchitectureRules.all().stream().map(ArchitectureRule::id).toList()
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("violations")
    void eachRuleReportsItsApprovedViolation(String ruleId, DefaultArchitectureRuleContext context) {
        ArchitectureRule rule = BuiltInArchitectureRules.all().stream()
                .filter(candidate -> candidate.id().equals(ruleId))
                .findFirst()
                .orElseThrow();

        List<ArchitectureFinding> findings = rule.evaluate(context);

        assertEquals(1, findings.size());
        assertEquals(ruleId, findings.getFirst().ruleId());
    }

    private static Stream<Arguments> violations() {
        return Stream.of(
                Arguments.of("ARCH-001", dependencyContext(ArchitectureLayer.DOMAIN,
                        ArchitectureLayer.APPLICATION, "sample.domain.Order",
                        "sample.application.OrderService", false)),
                Arguments.of("ARCH-002", dependencyContext(ArchitectureLayer.DOMAIN,
                        ArchitectureLayer.UNKNOWN, "sample.domain.Order",
                        "org.springframework.stereotype.Component", false)),
                Arguments.of("ARCH-003", dependencyContext(ArchitectureLayer.APPLICATION,
                        ArchitectureLayer.INFRASTRUCTURE, "sample.application.OrderService",
                        "sample.infrastructure.OrderRepositoryImpl", false)),
                Arguments.of("ARCH-004", dependencyContext(ArchitectureLayer.APPLICATION,
                        ArchitectureLayer.UNKNOWN, "sample.application.OrderService",
                        "org.apache.ibatis.session.SqlSession", false)),
                Arguments.of("ARCH-005", dependencyContext(ArchitectureLayer.FACADE,
                        ArchitectureLayer.DOMAIN, "sample.facade.OrderFacade",
                        "sample.domain.Order", false)),
                Arguments.of("ARCH-006", dependencyContext(ArchitectureLayer.STARTER,
                        ArchitectureLayer.APPLICATION, "sample.starter.SampleApplication",
                        "sample.application.OrderServiceImpl", false)),
                Arguments.of("ARCH-007", dependencyContext(ArchitectureLayer.COMMON,
                        ArchitectureLayer.DOMAIN, "sample.common.Result",
                        "sample.domain.Order", false)),
                Arguments.of("ARCH-008", dependencyContext(ArchitectureLayer.ADAPTER,
                        ArchitectureLayer.INFRASTRUCTURE, "sample.adapter.OrderController",
                        "sample.infrastructure.OrderRepositoryImpl", false)),
                Arguments.of("ARCH-009", dependencyContext(ArchitectureLayer.DOMAIN,
                        ArchitectureLayer.UNKNOWN, "sample.domain.Order",
                        "jakarta.persistence.Entity", false)),
                Arguments.of("ARCH-010", typeContext(new ArchitectureType(
                        "sample-facade", "sample.facade.OrderFacadeImpl",
                        ArchitectureLayer.FACADE, Set.of(), false)))
        );
    }

    private static DefaultArchitectureRuleContext dependencyContext(
            ArchitectureLayer sourceLayer,
            ArchitectureLayer targetLayer,
            String sourceClass,
            String targetClass,
            boolean targetInterface
    ) {
        ArchitectureType source = new ArchitectureType(
                "sample", sourceClass, sourceLayer, Set.of(), false);
        ArchitectureType target = new ArchitectureType(
                "sample", targetClass, targetLayer, Set.of(), targetInterface);
        ArchitectureDependency dependency = new ArchitectureDependency(
                "sample", sourceClass, "execute", "()V", sourceLayer,
                targetClass, "invoke", "()V", targetLayer,
                DependencyKind.METHOD_CALL, LocationKind.INSTRUCTION, 20);
        return new DefaultArchitectureRuleContext(new ArchitectureGraph(
                List.of(source, target), List.of(dependency)));
    }

    private static DefaultArchitectureRuleContext typeContext(ArchitectureType type) {
        return new DefaultArchitectureRuleContext(new ArchitectureGraph(List.of(type), List.of()));
    }
}
