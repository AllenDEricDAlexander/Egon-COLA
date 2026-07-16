package top.egon.cola.component.bytecode.maven.report;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureFinding;
import top.egon.cola.component.bytecode.maven.result.ArchitectureCheckResult;

import java.io.IOException;
import java.nio.file.Path;

public final class HtmlReportWriter implements ArchitectureReportWriter {

    @Override
    public void write(ArchitectureCheckResult result, Path outputDirectory) throws IOException {
        StringBuilder rows = new StringBuilder();
        for (ArchitectureFinding finding : result.findings()) {
            rows.append("<tr><td>").append(escape(finding.ruleId()))
                    .append("</td><td>").append(escape(String.valueOf(finding.severity())))
                    .append("</td><td>").append(escape(finding.module()))
                    .append("</td><td>").append(escape(finding.sourceClass()))
                    .append("</td><td>").append(escape(finding.sourceMember()))
                    .append("</td><td>").append(escape(finding.targetClass()))
                    .append("</td><td>").append(escape(finding.targetMember()))
                    .append("</td><td>").append(finding.lineNumber() == null ? -1 : finding.lineNumber())
                    .append("</td><td>").append(escape(finding.message()))
                    .append("</td></tr>");
        }
        String html = "<!doctype html><html lang=\"en\"><head><meta charset=\"UTF-8\">"
                + "<title>Egon COLA Architecture Report</title>"
                + "<style>body{font-family:sans-serif;margin:2rem}table{border-collapse:collapse;width:100%}"
                + "th,td{border:1px solid #ccc;padding:.4rem;text-align:left}</style></head><body>"
                + "<h1>Egon COLA Architecture Report</h1><p>total=" + result.total()
                + "</p><p>countsByRule=" + escape(result.countsByRule().toString())
                + "</p><p>countsBySeverity=" + escape(result.countsBySeverity().toString())
                + "</p><table><thead><tr><th>Rule</th><th>Severity</th><th>Module</th>"
                + "<th>Source class</th><th>Source member</th><th>Target class</th>"
                + "<th>Target member</th><th>Line</th><th>Message</th></tr></thead><tbody>"
                + rows + "</tbody></table></body></html>";
        AtomicReportFile.write(outputDirectory.resolve("architecture-report.html"), html);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
