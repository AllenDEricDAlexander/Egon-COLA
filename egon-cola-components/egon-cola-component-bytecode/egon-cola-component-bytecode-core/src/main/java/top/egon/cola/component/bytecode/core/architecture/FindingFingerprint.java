package top.egon.cola.component.bytecode.core.architecture;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureFinding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class FindingFingerprint {

    private FindingFingerprint() {
    }

    public static String of(ArchitectureFinding finding) {
        String canonical = String.join("\u001f",
                value(finding.ruleId()),
                value(finding.severity()),
                value(finding.module()),
                value(finding.sourceLayer()),
                value(finding.targetLayer()),
                value(finding.sourceClass()),
                value(finding.sourceMember()),
                value(finding.sourceDescriptor()),
                value(finding.targetClass()),
                value(finding.targetMember()),
                value(finding.targetDescriptor()),
                value(finding.dependencyKind()),
                value(finding.locationKind()));
        return sha256(canonical.getBytes(StandardCharsets.UTF_8));
    }

    private static String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the JDK", exception);
        }
    }
}
