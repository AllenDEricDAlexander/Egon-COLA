package top.egon.cola.component.bytecode.maven.report;

import top.egon.cola.component.bytecode.maven.result.ArchitectureCheckResult;

import java.io.IOException;
import java.nio.file.Path;

public interface ArchitectureReportWriter {

    void write(ArchitectureCheckResult result, Path outputDirectory) throws IOException;
}
