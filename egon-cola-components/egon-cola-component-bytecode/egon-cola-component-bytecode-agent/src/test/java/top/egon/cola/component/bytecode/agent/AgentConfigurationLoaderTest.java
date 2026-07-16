package top.egon.cola.component.bytecode.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentConfigurationLoaderTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void appliesAgentArgumentsThenYamlThenSystemAndEnvironmentThenDefaults() throws Exception {
        Path yaml = temporaryDirectory.resolve("agent.yml");
        Files.writeString(yaml, """
                enabled: true
                features: observation
                include: yaml.application.*
                exclude: yaml.application.generated.*
                """);
        Properties system = new Properties();
        system.setProperty("egon.cola.bytecode.enabled", "false");
        system.setProperty("egon.cola.bytecode.features", "access-guard");
        system.setProperty("egon.cola.bytecode.include", "system.application.*");
        Map<String, String> environment = Map.of(
                "EGON_COLA_BYTECODE_ENABLED", "false",
                "EGON_COLA_BYTECODE_FEATURES", "method-extension",
                "EGON_COLA_BYTECODE_INCLUDE", "environment.application.*"
        );

        AgentConfiguration yamlConfiguration = new AgentConfigurationLoader(system, environment)
                .load("config=" + yaml);
        AgentConfiguration argumentConfiguration = new AgentConfigurationLoader(system, environment)
                .load("enabled=true,features=executor,method-extension,"
                        + "include=argument.one.*,argument.two.*,config=" + yaml);

        assertEquals(BridgeCapability.OBSERVATION, yamlConfiguration.features().iterator().next());
        assertEquals("yaml.application.*", yamlConfiguration.includes().getFirst());
        assertEquals("yaml.application.generated.*", yamlConfiguration.excludes().getFirst());
        assertEquals(
                java.util.Set.of(BridgeCapability.EXECUTOR, BridgeCapability.METHOD_EXTENSION),
                argumentConfiguration.features());
        assertEquals(java.util.List.of("argument.one.*", "argument.two.*"),
                argumentConfiguration.includes());
        assertTrue(argumentConfiguration.methodExtensionEnabled());
    }

    @Test
    void requiresExplicitIncludeWhenEnabled() {
        assertThrows(IllegalArgumentException.class,
                () -> new AgentConfigurationLoader(new Properties(), Map.of())
                        .load("enabled=true,features=executor"));
    }

    @Test
    void loadsObservationMatchConfigurationAtTransformTime() {
        AgentConfiguration configuration = new AgentConfigurationLoader(
                new Properties(), Map.of()).load(
                "enabled=true,features=observation,include=sample.*,"
                        + "observation-include=sample.application.*,"
                        + "observation-method=find*,save*,"
                        + "observation-exclude=sample.application.Internal#*,"
                        + "observe-constructors=true,"
                        + "observation-slow-threshold-millis=25");

        assertEquals(java.util.List.of("sample.application.*"),
                configuration.observationIncludes());
        assertEquals(java.util.List.of("find*", "save*"),
                configuration.observationMethods());
        assertEquals(java.util.List.of("sample.application.Internal#*"),
                configuration.observationExcludes());
        assertEquals(true, configuration.observeConstructors());
        assertEquals(25L, configuration.observationSlowThresholdMillis());
    }

    @Test
    void appliesMalformedYamlFailurePolicy() throws Exception {
        Path yaml = temporaryDirectory.resolve("broken.yml");
        Files.writeString(yaml, "enabled: [not-closed");

        AgentConfiguration degraded = new AgentConfigurationLoader(new Properties(), Map.of())
                .load("enabled=true,include=application.*,failure-policy=skip-class,config=" + yaml);

        assertEquals(AgentFailurePolicy.SKIP_CLASS, degraded.failurePolicy());
        assertThrows(IllegalArgumentException.class,
                () -> new AgentConfigurationLoader(new Properties(), Map.of())
                        .load("enabled=true,include=application.*,failure-policy=mark-fatal,config=" + yaml));
    }

    @Test
    void startupSummaryExposesOnlyCountsAndDigests() {
        AgentConfiguration configuration = new AgentConfigurationLoader(new Properties(), Map.of())
                .load("enabled=true,features=executor,include=secret.application.*,"
                        + "exclude=secret.application.generated.*");

        String summary = new AgentStartupReporter().startupSummary(configuration, "ACTIVE", true);

        assertFalse(summary.contains("secret.application"));
        assertFalse(summary.contains("generated"));
        assertEquals(16, configuration.includeDigest().length());
        assertEquals(16, configuration.excludeDigest().length());
    }
}
