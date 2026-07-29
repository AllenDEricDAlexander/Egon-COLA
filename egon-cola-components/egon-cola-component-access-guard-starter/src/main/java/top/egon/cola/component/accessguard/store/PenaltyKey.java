package top.egon.cola.component.accessguard.store;

public record PenaltyKey(String ruleId, String stateVersion, String keyHash) {

    public PenaltyKey {
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("ruleId must not be blank");
        }
        if (stateVersion == null || stateVersion.isBlank()) {
            throw new IllegalArgumentException("stateVersion must not be blank");
        }
        if (keyHash == null || !keyHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("keyHash must be a lowercase SHA-256 value");
        }
        ruleId = ruleId.trim();
        stateVersion = stateVersion.trim();
    }

    @Override
    public String toString() {
        return "PenaltyKey[ruleId=" + ruleId + ", stateVersion=" + stateVersion + ", keyHash=<redacted>]";
    }
}
