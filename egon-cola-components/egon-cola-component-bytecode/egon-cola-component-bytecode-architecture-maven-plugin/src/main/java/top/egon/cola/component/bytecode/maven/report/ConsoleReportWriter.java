package top.egon.cola.component.bytecode.maven.report;

import org.apache.maven.plugin.logging.Log;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureFinding;
import top.egon.cola.component.bytecode.maven.result.ArchitectureCheckResult;

import java.nio.file.Path;
import java.util.Objects;

public final class ConsoleReportWriter implements ArchitectureReportWriter {

    private final Log log;

    public ConsoleReportWriter(Log log) {
        this.log = Objects.requireNonNull(log, "log");
    }

    @Override
    public void write(ArchitectureCheckResult result, Path outputDirectory) {
        log.info("Egon COLA architecture: total=" + result.total()
                + ", byRule=" + result.countsByRule()
                + ", bySeverity=" + result.countsBySeverity());
        for (ArchitectureFinding finding : result.findings()) {
            log.info(ReportText.findingLine(finding));
        }
    }
}
