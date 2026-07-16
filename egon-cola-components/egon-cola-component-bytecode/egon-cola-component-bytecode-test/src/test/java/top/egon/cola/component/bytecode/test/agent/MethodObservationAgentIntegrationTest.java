package top.egon.cola.component.bytecode.test.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodObservationAgentIntegrationTest {

    @Test
    void verifiesObservationSemanticsInARealPremainProcess() throws Exception {
        String arguments = "enabled=true,features=observation,"
                + "include=sample.bytecode.agent.*,"
                + "observation-include=sample.bytecode.agent.*,"
                + "observation-method=*,observe-constructors=true,"
                + "observation-slow-threshold-millis=0";
        ForkedAgentProcess.Result result = new ForkedAgentProcess()
                .run("sample.bytecode.agent.ObservationAgentFixture", arguments);
        String output = result.combinedOutput();

        assertEquals(0, result.exitCode(), output);
        assertTrue(output.contains("Egon bytecode Agent state=ACTIVE"), output);
        assertTrue(output.contains("effectiveFeatures=[OBSERVATION]"), output);
        assertTrue(output.contains("OBSERVATION_AGENT_OK"), output);
        assertFalse(output.contains("password=secret"), output);
        assertFalse(output.contains("sample.bytecode.agent.*"), output);
    }
}
