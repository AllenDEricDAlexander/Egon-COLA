package top.egon.cola.component.gateway.core.context;

public record GatewayGovernanceDecision(
        String policyId,
        String decisionCode,
        boolean allowed
) {

    public GatewayGovernanceDecision {
        policyId = required(policyId, "policyId");
        decisionCode = required(decisionCode, "decisionCode");
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
