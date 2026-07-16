package top.egon.cola.component.bytecode.test.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodExtensionAgentIntegrationTest {

    @Test
    void verifiesMethodExtensionSemanticsInARealPremainProcess() throws Exception {
        String arguments = "enabled=true,features=method-extension,observation,"
                + "include=sample.bytecode.agent.*,"
                + "observation-include=sample.bytecode.agent.MethodExtensionAgentFixture$Target,"
                + "observation-method=allowIdentity,observedReject";
        ForkedAgentProcess.Result result = new ForkedAgentProcess()
                .run("sample.bytecode.agent.MethodExtensionAgentFixture", arguments);
        String output = result.combinedOutput();

        assertEquals(0, result.exitCode(), output);
        assertTrue(output.contains("Egon bytecode Agent state=ACTIVE"), output);
        assertTrue(output.contains("OBSERVATION"), output);
        assertTrue(output.contains("METHOD_EXTENSION"), output);
        assertTrue(output.contains("METHOD_EXTENSION_AGENT_OK"), output);
        assertFalse(output.contains("password=secret"), output);
        assertFalse(output.contains("sample.bytecode.agent.*"), output);
    }
}
