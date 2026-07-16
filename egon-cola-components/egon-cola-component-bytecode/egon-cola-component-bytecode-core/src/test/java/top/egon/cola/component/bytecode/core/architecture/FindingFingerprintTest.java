package top.egon.cola.component.bytecode.core.architecture;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureFinding;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureLayer;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureSeverity;
import top.egon.cola.component.bytecode.api.architecture.DependencyKind;
import top.egon.cola.component.bytecode.api.architecture.LocationKind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FindingFingerprintTest {

    @Test
    void excludesPresentationFieldsButIncludesStructuralTargetIdentity() {
        ArchitectureFinding original = finding("target.Type", "run", 10, "message", "suggestion");
        ArchitectureFinding presentationChanged = finding(
                "target.Type", "run", 99, "different", "different");
        ArchitectureFinding targetChanged = finding(
                "target.Other", "run", 10, "message", "suggestion");
        ArchitectureFinding memberChanged = finding(
                "target.Type", "other", 10, "message", "suggestion");

        assertEquals(FindingFingerprint.of(original), FindingFingerprint.of(presentationChanged));
        assertNotEquals(FindingFingerprint.of(original), FindingFingerprint.of(targetChanged));
        assertNotEquals(FindingFingerprint.of(original), FindingFingerprint.of(memberChanged));
    }

    private ArchitectureFinding finding(
            String targetClass,
            String targetMember,
            int line,
            String message,
            String suggestion
    ) {
        return new ArchitectureFinding(
                "ARCH-001", ArchitectureSeverity.ERROR, "sample-domain",
                ArchitectureLayer.DOMAIN, ArchitectureLayer.APPLICATION,
                "sample.Order", "execute", "()V", targetClass,
                targetMember, "()V", DependencyKind.METHOD_CALL,
                LocationKind.INSTRUCTION, line, message, suggestion
        );
    }
}
