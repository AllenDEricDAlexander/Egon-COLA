package top.egon.cola.component.bytecode.test.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessGuardAgentIntegrationTest {

    @Test
    void verifiesAccessGuardMatrixInARealPremainProcess() throws Exception {
        String arguments = "enabled=true,features=access-guard,observation,"
                + "include=sample.bytecode.agent.*,"
                + "observation-include=sample.bytecode.agent.AccessGuardAgentFixture$Target,"
                + "observation-method=*";
        ForkedAgentProcess.Result result = new ForkedAgentProcess()
                .run("sample.bytecode.agent.AccessGuardAgentFixture", arguments);
        String output = result.combinedOutput();

        assertEquals(0, result.exitCode(), output);
        assertTrue(output.contains("Egon bytecode Agent state=ACTIVE"), output);
        assertTrue(output.contains("ACCESS_GUARD"), output);
        assertTrue(output.contains("ACCESS_GUARD_AGENT_OK"), output);
        assertFalse(output.contains("password=secret"), output);
        assertFalse(output.contains("sample.bytecode.agent.*"), output);
    }
}
