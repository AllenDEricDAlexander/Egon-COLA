package top.egon.cola.component.bytecode.maven.rule;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchitectureRuleLoaderTest {

    @Test
    void builtInsAreLoadedFirstAndContainExactlyTheApprovedRules() {
        assertEquals(
                java.util.List.of("ARCH-001", "ARCH-002", "ARCH-003", "ARCH-004", "ARCH-005",
                        "ARCH-006", "ARCH-007", "ARCH-008", "ARCH-009", "ARCH-010"),
                new ArchitectureRuleLoader().load(Thread.currentThread().getContextClassLoader())
                        .stream().map(rule -> rule.id()).toList()
        );
    }
}
