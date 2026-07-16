package top.egon.cola.component.bytecode.maven.report;

import top.egon.cola.component.bytecode.maven.result.ArchitectureCheckResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

public final class TextReportWriter implements ArchitectureReportWriter {

    @Override
    public void write(ArchitectureCheckResult result, Path outputDirectory) throws IOException {
        String findings = result.findings().stream()
                .map(ReportText::findingLine)
                .collect(Collectors.joining(System.lineSeparator()));
        String content = "total=" + result.total() + System.lineSeparator()
                + "countsByRule=" + result.countsByRule() + System.lineSeparator()
                + "countsBySeverity=" + result.countsBySeverity() + System.lineSeparator()
                + findings + (findings.isEmpty() ? "" : System.lineSeparator());
        AtomicReportFile.write(outputDirectory.resolve("architecture-report.txt"), content);
    }
}
