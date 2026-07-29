package top.egon.cola.component.accessguard.core.plan;

public record ObservabilityConfig(
        boolean finalEvents,
        boolean stageEvents,
        boolean metrics,
        boolean logging,
        boolean endpoint
) {

    public static ObservabilityConfig defaults() {
        return new ObservabilityConfig(true, false, true, true, true);
    }
}
