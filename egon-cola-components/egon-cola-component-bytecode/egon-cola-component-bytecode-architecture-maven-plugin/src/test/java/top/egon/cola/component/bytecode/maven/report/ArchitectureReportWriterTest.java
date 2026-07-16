package top.egon.cola.component.bytecode.maven.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureFinding;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureLayer;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureSeverity;
import top.egon.cola.component.bytecode.api.architecture.DependencyKind;
import top.egon.cola.component.bytecode.api.architecture.LocationKind;
import top.egon.cola.component.bytecode.maven.result.ArchitectureCheckResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureReportWriterTest {

    @TempDir
    Path outputDirectory;

    @Test
    void allFormatsExposeTheSameDeterministicTotalsAndEscapeValues() throws Exception {
        ArchitectureCheckResult result = ArchitectureCheckResult.from(List.of(
                finding("ARCH-002", ArchitectureSeverity.WARNING, "b", "run", 12,
                        "org.example.<Framework>"),
                finding("ARCH-001", ArchitectureSeverity.ERROR, "a", null, null,
                        "org.example.\"Application\"")
        ));
        CapturingLog log = new CapturingLog();

        new ConsoleReportWriter(log).write(result, outputDirectory);
        new TextReportWriter().write(result, outputDirectory);
        new JsonReportWriter(new ObjectMapper()).write(result, outputDirectory);
        new HtmlReportWriter().write(result, outputDirectory);

        String text = Files.readString(outputDirectory.resolve("architecture-report.txt"));
        JsonNode json = new ObjectMapper().readTree(
                outputDirectory.resolve("architecture-report.json").toFile());
        String html = Files.readString(outputDirectory.resolve("architecture-report.html"));

        assertEquals(List.of("ARCH-001", "ARCH-002"),
                result.findings().stream().map(ArchitectureFinding::ruleId).toList());
        assertEquals(2, json.path("summary").path("total").asInt());
        assertEquals(1, json.path("summary").path("countsByRule").path("ARCH-001").asInt());
        assertEquals(1, json.path("summary").path("countsBySeverity").path("ERROR").asInt());
        assertTrue(log.messages.stream().anyMatch(message -> message.contains("total=2")));
        assertTrue(text.contains("ARCH-001=1"));
        assertTrue(text.contains("ERROR=1"));
        assertTrue(text.contains("<class>"));
        assertTrue(text.contains("line=-1"));
        assertTrue(html.contains("ARCH-001"));
        assertTrue(html.contains("ERROR"));
        assertTrue(html.contains("org.example.&lt;Framework&gt;"));
        assertTrue(!html.contains("org.example.<Framework>"));
    }

    private ArchitectureFinding finding(
            String ruleId,
            ArchitectureSeverity severity,
            String sourceClass,
            String sourceMember,
            Integer line,
            String targetClass
    ) {
        return new ArchitectureFinding(
                ruleId, severity, "sample", ArchitectureLayer.DOMAIN,
                ArchitectureLayer.APPLICATION, sourceClass, sourceMember, null,
                targetClass, null, null, DependencyKind.METHOD_CALL,
                LocationKind.INSTRUCTION, line, "message <unsafe>", "suggestion & safe"
        );
    }

    private static final class CapturingLog implements Log {

        private final List<String> messages = new ArrayList<>();

        @Override public boolean isDebugEnabled() { return true; }
        @Override public void debug(CharSequence content) { messages.add(content.toString()); }
        @Override public void debug(CharSequence content, Throwable error) { debug(content); }
        @Override public void debug(Throwable error) { messages.add(error.toString()); }
        @Override public boolean isInfoEnabled() { return true; }
        @Override public void info(CharSequence content) { messages.add(content.toString()); }
        @Override public void info(CharSequence content, Throwable error) { info(content); }
        @Override public void info(Throwable error) { messages.add(error.toString()); }
        @Override public boolean isWarnEnabled() { return true; }
        @Override public void warn(CharSequence content) { messages.add(content.toString()); }
        @Override public void warn(CharSequence content, Throwable error) { warn(content); }
        @Override public void warn(Throwable error) { messages.add(error.toString()); }
        @Override public boolean isErrorEnabled() { return true; }
        @Override public void error(CharSequence content) { messages.add(content.toString()); }
        @Override public void error(CharSequence content, Throwable error) { error(content); }
        @Override public void error(Throwable error) { messages.add(error.toString()); }
    }
}
