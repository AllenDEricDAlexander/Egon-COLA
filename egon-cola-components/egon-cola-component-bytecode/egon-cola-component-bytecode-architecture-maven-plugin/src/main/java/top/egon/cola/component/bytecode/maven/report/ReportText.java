package top.egon.cola.component.bytecode.maven.report;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureFinding;

final class ReportText {

    private ReportText() {
    }

    static String findingLine(ArchitectureFinding finding) {
        return finding.ruleId() + " [" + finding.severity() + "] "
                + value(finding.sourceClass(), "<class>") + "#"
                + value(finding.sourceMember(), "<class>")
                + " -> " + value(finding.targetClass(), "<none>") + "#"
                + value(finding.targetMember(), "<none>")
                + " line=" + (finding.lineNumber() == null ? -1 : finding.lineNumber())
                + " " + value(finding.message(), "");
    }

    private static String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
