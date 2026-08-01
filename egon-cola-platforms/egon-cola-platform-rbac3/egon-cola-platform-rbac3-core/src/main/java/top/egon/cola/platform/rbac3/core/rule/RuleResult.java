package top.egon.cola.platform.rbac3.core.rule;

import top.egon.cola.platform.rbac3.contract.authorization.Decision;

import java.util.List;
import java.util.Map;

public record RuleResult(
        Decision decision,
        String reasonCode,
        List<String> evidenceIds,
        Map<String, String> safeArguments
) {

    public RuleResult {
        if (decision == null || reasonCode == null || reasonCode.isBlank()) {
            throw new IllegalArgumentException("decision and reasonCode are required");
        }
        evidenceIds = List.copyOf(evidenceIds);
        safeArguments = Map.copyOf(safeArguments);
    }

    public static RuleResult allow(String reasonCode) {
        return new RuleResult(Decision.ALLOW, reasonCode, List.of(), Map.of());
    }

    public static RuleResult deny(String reasonCode, List<String> evidenceIds) {
        return new RuleResult(Decision.DENY, reasonCode, evidenceIds, Map.of());
    }

    public boolean allowed() {
        return decision == Decision.ALLOW;
    }
}
