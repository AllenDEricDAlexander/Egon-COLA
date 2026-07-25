package top.egon.cola.component.gateway.engine.discovery;

import top.egon.cola.component.gateway.core.provider.ProviderInstance;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ProviderCandidateFilterResult(
        List<ProviderInstance> candidates,
        Map<ProviderCandidateStage, Integer> counts,
        Map<String, String> rejectedReasons
) {

    public ProviderCandidateFilterResult {
        candidates = List.copyOf(
                Objects.requireNonNull(candidates, "candidates")
        );
        counts = Map.copyOf(Objects.requireNonNull(counts, "counts"));
        rejectedReasons = Map.copyOf(
                Objects.requireNonNull(rejectedReasons, "rejectedReasons")
        );
    }
}
