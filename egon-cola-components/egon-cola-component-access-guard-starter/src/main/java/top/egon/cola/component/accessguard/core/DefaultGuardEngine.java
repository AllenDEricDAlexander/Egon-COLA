package top.egon.cola.component.accessguard.core;

import java.util.Objects;

public final class DefaultGuardEngine implements GuardEngine {

    private final GuardAdmissionPipeline admissionPipeline;
    private final GuardExecutionCoordinator executionCoordinator;

    public DefaultGuardEngine(
            GuardAdmissionPipeline admissionPipeline,
            GuardExecutionCoordinator executionCoordinator
    ) {
        this.admissionPipeline = Objects.requireNonNull(admissionPipeline, "admissionPipeline");
        this.executionCoordinator = Objects.requireNonNull(executionCoordinator, "executionCoordinator");
    }

    @Override
    public GuardOutcome evaluate(GuardInvocation invocation) {
        PreparedGuardExecution prepared = prepare(invocation);
        prepared.finish(prepared.admission());
        return prepared.admission();
    }

    @Override
    public PreparedGuardExecution prepare(GuardInvocation invocation) {
        GuardAdmission admission = admissionPipeline.evaluate(Objects.requireNonNull(invocation, "invocation"));
        return executionCoordinator.prepare(invocation, admission);
    }

    @Override
    public Object execute(GuardInvocation invocation) throws Throwable {
        return executionCoordinator.execute(prepare(invocation)).value();
    }
}
