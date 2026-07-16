package top.egon.cola.component.bytecode.agent;

import top.egon.cola.component.bytecode.agent.transform.CompositeBytecodeTransformer;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.core.enhance.ApplicationClassEnhancer;
import top.egon.cola.component.bytecode.core.enhance.accessguard.AccessGuardMatcher;
import top.egon.cola.component.bytecode.core.enhance.methodextension.MethodExtensionMatcher;
import top.egon.cola.component.bytecode.core.enhance.observation.ObservationMatcher;

import java.lang.instrument.Instrumentation;

public final class BytecodeAgent {

    private BytecodeAgent() {
    }

    public static void premain(String arguments, Instrumentation instrumentation) {
        AgentStateStore stateStore = AgentStateStore.global();
        AgentStartupReporter reporter = new AgentStartupReporter();
        try {
            AgentConfiguration configuration = new AgentConfigurationLoader().load(arguments);
            if (!configuration.enabled()) {
                System.out.println(reporter.startupSummary(
                        configuration, AgentState.DISABLED.name(), true));
                return;
            }
            stateStore.start(configuration);
            ObservationMatcher observationMatcher = configuration.features()
                    .contains(BridgeCapability.OBSERVATION)
                    ? new ObservationMatcher(
                    configuration.observationIncludes(),
                    configuration.observationMethods(),
                    configuration.observationExcludes(),
                    configuration.observeConstructors(),
                    configuration.observationSlowThresholdMillis() < 0L
                            ? -1L
                            : Math.multiplyExact(
                            configuration.observationSlowThresholdMillis(), 1_000_000L))
                    : null;
            ApplicationClassEnhancer applicationEnhancer = new ApplicationClassEnhancer(
                    configuration.features().contains(BridgeCapability.EXECUTOR),
                    observationMatcher,
                    configuration.methodExtensionEnabled()
                            ? new MethodExtensionMatcher() : null,
                    configuration.accessGuardEnabled()
                            ? new AccessGuardMatcher() : null
            );
            CompositeBytecodeTransformer transformer = new CompositeBytecodeTransformer(
                    new ClassNameFilter(configuration),
                    configuration,
                    stateStore,
                    (loader, className, classfileBuffer) ->
                            applicationEnhancer.enhance(loader, classfileBuffer)
            );
            instrumentation.addTransformer(transformer, false);
            stateStore.active();
            System.out.println(reporter.startupSummary(
                    configuration, AgentState.ACTIVE.name(), true));
        } catch (Throwable failure) {
            if (stateStore.state() == AgentState.DISABLED) {
                stateStore.startupFailed(AgentFailure.transform(
                        BytecodeAgent.class.getClassLoader(),
                        BytecodeAgent.class.getName(),
                        "STARTUP",
                        failure,
                        AgentFailurePolicy.MARK_FATAL
                ));
            } else if (stateStore.state() != AgentState.FAILED) {
                stateStore.recordFailure(AgentFailure.transform(
                        BytecodeAgent.class.getClassLoader(),
                        BytecodeAgent.class.getName(),
                        "STARTUP",
                        failure,
                        AgentFailurePolicy.MARK_FATAL
                ));
                stateStore.failed();
            }
            System.err.println(reporter.failureSummary(failure));
        }
    }
}
