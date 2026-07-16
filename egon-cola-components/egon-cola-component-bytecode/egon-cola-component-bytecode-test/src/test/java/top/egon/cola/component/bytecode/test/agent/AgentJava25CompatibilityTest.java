package top.egon.cola.component.bytecode.test.agent;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AgentJava25CompatibilityTest {

    @Test
    void runsOnJava25WhenJava25HomeIsAvailable() throws Exception {
        String java25Home = System.getenv("JAVA25_HOME");
        assumeTrue(java25Home != null && !java25Home.isBlank(),
                "JAVA25_HOME is not configured; CI compatibility job supplies it");
        Path javaExecutable = Path.of(java25Home, "bin", "java");
        assumeTrue(Files.isExecutable(javaExecutable), "JAVA25_HOME/bin/java is not executable");

        ForkedAgentProcess.Result result = new ForkedAgentProcess()
                .run(javaExecutable, "sample.bytecode.agent.ExecutorAgentFixture");

        assertEquals(0, result.exitCode(), result.combinedOutput());
    }
}
