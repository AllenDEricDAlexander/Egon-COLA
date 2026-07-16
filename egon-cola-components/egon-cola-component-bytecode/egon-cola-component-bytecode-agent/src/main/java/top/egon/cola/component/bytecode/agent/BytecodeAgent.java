package top.egon.cola.component.bytecode.agent;

import top.egon.cola.component.bytecode.agent.transform.CompositeBytecodeTransformer;

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
            CompositeBytecodeTransformer transformer = new CompositeBytecodeTransformer(
                    new ClassNameFilter(configuration),
                    configuration,
                    stateStore,
                    (loader, className, classfileBuffer) -> null
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
