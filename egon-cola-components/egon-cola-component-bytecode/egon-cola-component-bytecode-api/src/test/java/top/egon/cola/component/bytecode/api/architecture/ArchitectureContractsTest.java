package top.egon.cola.component.bytecode.api.architecture;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ArchitectureContractsTest {

    @Test
    void findingContractKeepsStableFieldOrder() {
        assertEquals(
                java.util.List.of("ruleId", "severity", "module", "sourceLayer", "targetLayer",
                        "sourceClass", "sourceMember", "sourceDescriptor", "targetClass",
                        "targetMember", "targetDescriptor", "dependencyKind", "locationKind",
                        "lineNumber", "message", "suggestion"),
                Arrays.stream(ArchitectureFinding.class.getRecordComponents())
                        .map(RecordComponent::getName)
                        .toList()
        );
    }

    @Test
    void publicApiDoesNotExposeAsmOrMavenTypes() {
        String signatures = Arrays.stream(ArchitectureRule.class.getMethods())
                .map(Object::toString)
                .reduce("", String::concat);
        assertFalse(signatures.contains("org.objectweb.asm"));
        assertFalse(signatures.contains("org.apache.maven"));
    }
}
