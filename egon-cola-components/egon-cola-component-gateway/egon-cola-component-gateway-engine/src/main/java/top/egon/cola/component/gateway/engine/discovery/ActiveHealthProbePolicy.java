package top.egon.cola.component.gateway.engine.discovery;

import top.egon.cola.component.gateway.core.provider.ProviderInstance;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public record ActiveHealthProbePolicy(
        boolean enabled,
        Duration interval,
        double jitterRatio,
        Duration timeout,
        int maximumConcurrency,
        int failureThreshold,
        int successThreshold,
        String httpMethod,
        String httpPath,
        Set<Integer> httpSuccessStatuses,
        String rpcServiceName,
        boolean rpcConnectFallback
) {

    public ActiveHealthProbePolicy {
        if (interval == null || interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException(
                    "active health interval must be positive"
            );
        }
        if (jitterRatio < 0 || jitterRatio > 1) {
            throw new IllegalArgumentException(
                    "active health jitterRatio must be between 0 and 1"
            );
        }
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException(
                    "active health timeout must be positive"
            );
        }
        if (maximumConcurrency < 1
                || failureThreshold < 1
                || successThreshold < 1) {
            throw new IllegalArgumentException(
                    "active health limits and thresholds must be positive"
            );
        }
        httpMethod = required(httpMethod, "httpMethod")
                .toUpperCase(Locale.ROOT);
        httpPath = path(httpPath);
        httpSuccessStatuses = Set.copyOf(httpSuccessStatuses);
        if (httpSuccessStatuses.isEmpty()) {
            throw new IllegalArgumentException(
                    "httpSuccessStatuses must not be empty"
            );
        }
        if (httpSuccessStatuses.stream()
                .anyMatch(status -> status < 100 || status > 599)) {
            throw new IllegalArgumentException(
                    "httpSuccessStatuses must contain valid HTTP statuses"
            );
        }
        rpcServiceName = rpcServiceName == null
                ? ""
                : rpcServiceName.trim();
    }

    public static ActiveHealthProbePolicy defaults() {
        return new ActiveHealthProbePolicy(
                true,
                Duration.ofSeconds(10),
                0.2,
                Duration.ofSeconds(2),
                16,
                2,
                2,
                "GET",
                "/actuator/health",
                Set.of(200),
                "",
                true
        );
    }

    public String httpMethod(ProviderInstance instance) {
        return instance.metadata().getOrDefault(
                "gateway.health.method",
                httpMethod
        ).toUpperCase(Locale.ROOT);
    }

    public String httpPath(ProviderInstance instance) {
        return path(instance.metadata().getOrDefault(
                "gateway.health.path",
                httpPath
        ));
    }

    public Set<Integer> httpSuccessStatuses(ProviderInstance instance) {
        String configured = instance.metadata().get(
                "gateway.health.success-statuses"
        );
        if (configured == null || configured.isBlank()) {
            return httpSuccessStatuses;
        }
        Set<Integer> statuses = new LinkedHashSet<>();
        for (String value : configured.split(",")) {
            int status = Integer.parseInt(value.trim());
            if (status < 100 || status > 599) {
                throw new IllegalArgumentException(
                        "gateway health status must be a valid HTTP status"
                );
            }
            statuses.add(status);
        }
        return Set.copyOf(statuses);
    }

    public String rpcServiceName(ProviderInstance instance) {
        return instance.metadata().getOrDefault(
                "gateway.health.rpc-service",
                rpcServiceName
        );
    }

    private static String path(String value) {
        String result = required(value, "httpPath");
        if (!result.startsWith("/") || result.startsWith("//")) {
            throw new IllegalArgumentException(
                    "active health HTTP path must be absolute-path only"
            );
        }
        return result;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
