package top.egon.cola.component.bytecode.maven.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.component.bytecode.maven.result.ArchitectureCheckResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class JsonReportWriter implements ArchitectureReportWriter {

    private final ObjectMapper objectMapper;

    public JsonReportWriter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public void write(ArchitectureCheckResult result, Path outputDirectory) throws IOException {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", result.total());
        summary.put("countsByRule", result.countsByRule());
        summary.put("countsBySeverity", result.countsBySeverity());
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("summary", summary);
        document.put("findings", result.findings());
        AtomicReportFile.write(outputDirectory.resolve("architecture-report.json"),
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(document));
    }
}
