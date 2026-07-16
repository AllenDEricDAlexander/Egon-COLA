package top.egon.cola.component.bytecode.test.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutorAgentIntegrationTest {

    @Test
    void verifiesExecutorSemanticsInARealPremainProcess() throws Exception {
        ForkedAgentProcess.Result result = new ForkedAgentProcess()
                .run("sample.bytecode.agent.ExecutorAgentFixture");
        String output = result.combinedOutput();

        assertEquals(0, result.exitCode(), output);
        assertTrue(output.contains("Egon bytecode Agent state=ACTIVE"), output);
        assertTrue(output.contains("protocol=1.0"), output);
        assertTrue(output.contains("effectiveFeatures=[EXECUTOR]"), output);
        assertTrue(output.contains("EXECUTOR_AGENT_OK"), output);
        assertFalse(output.contains("sample.bytecode.agent.*"), output);
    }
}
