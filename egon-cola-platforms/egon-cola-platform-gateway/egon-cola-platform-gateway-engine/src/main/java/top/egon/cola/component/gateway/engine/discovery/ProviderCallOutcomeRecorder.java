package top.egon.cola.component.gateway.engine.discovery;

@FunctionalInterface
public interface ProviderCallOutcomeRecorder {

    void record(String runtimeIdentity, ProviderCallOutcome outcome);

    static ProviderCallOutcomeRecorder noop() {
        return (runtimeIdentity, outcome) -> {
        };
    }
}
