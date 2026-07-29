package top.egon.cola.component.accessguard.context;

public record AccessGuardResult(
        boolean passed,
        AccessGuardDecision decision,
        String ruleName,
        String accessKeyHash,
        String message
) {

    public static AccessGuardResult pass(String ruleName, String accessKeyHash) {
        return new AccessGuardResult(true, AccessGuardDecision.PASS, ruleName, accessKeyHash, "pass");
    }

    public static AccessGuardResult reject(AccessGuardDecision decision, String ruleName, String accessKeyHash, String message) {
        return new AccessGuardResult(false, decision, ruleName, accessKeyHash, message);
    }
}
