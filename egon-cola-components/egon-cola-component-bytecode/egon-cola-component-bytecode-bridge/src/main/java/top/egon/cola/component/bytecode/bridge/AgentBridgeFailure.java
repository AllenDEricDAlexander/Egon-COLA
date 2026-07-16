package top.egon.cola.component.bytecode.bridge;

public record AgentBridgeFailure(
        String timestamp,
        String className,
        String classLoader,
        String feature,
        String exceptionType,
        String message,
        String policy
) {
    public AgentBridgeFailure {
        timestamp = safe(timestamp);
        className = safe(className);
        classLoader = safe(classLoader);
        feature = safe(feature);
        exceptionType = safe(exceptionType);
        message = safe(message);
        policy = safe(policy);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
