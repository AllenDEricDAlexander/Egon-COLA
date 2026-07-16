package top.egon.cola.component.bytecode.agent;

import java.time.Instant;

public record AgentFailure(
        Instant timestamp,
        String className,
        String classLoader,
        String feature,
        String exceptionType,
        String message,
        AgentFailurePolicy policy
) {
    private static final int MAXIMUM_MESSAGE_LENGTH = 256;

    public AgentFailure {
        timestamp = timestamp == null ? Instant.now() : timestamp;
        className = safe(className);
        classLoader = safe(classLoader);
        feature = safe(feature);
        exceptionType = safe(exceptionType);
        message = sanitize(message);
        policy = policy == null ? AgentFailurePolicy.SKIP_CLASS : policy;
    }

    public static AgentFailure transform(
            ClassLoader loader,
            String internalClassName,
            String feature,
            Throwable failure,
            AgentFailurePolicy policy
    ) {
        String loaderDescription = loader == null
                ? "bootstrap" : loader.getClass().getName() + "@"
                + Integer.toHexString(System.identityHashCode(loader));
        return new AgentFailure(
                Instant.now(),
                internalClassName == null ? "" : internalClassName.replace('/', '.'),
                loaderDescription,
                feature,
                failure == null ? "" : failure.getClass().getName(),
                failure == null ? "" : failure.getMessage(),
                policy
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String sanitize(String value) {
        String sanitized = safe(value).replace('\r', ' ').replace('\n', ' ');
        return sanitized.length() <= MAXIMUM_MESSAGE_LENGTH
                ? sanitized : sanitized.substring(0, MAXIMUM_MESSAGE_LENGTH);
    }
}
