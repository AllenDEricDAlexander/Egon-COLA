package top.egon.cola.component.ddc.admin.security.rpc;

import org.springframework.util.PatternMatchUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public record DdcHmacCredential(
        String credentialId,
        String accessKey,
        String secret,
        String clientType,
        Set<String> appCodePatterns,
        Set<String> envPatterns,
        Set<String> bizCodePatterns,
        Set<String> allowedOperations
) {

    public DdcHmacCredential {
        credentialId = required(credentialId, "credentialId");
        accessKey = required(accessKey, "accessKey");
        secret = required(secret, "secret");
        clientType = required(clientType, "clientType")
                .toUpperCase(Locale.ROOT);
        appCodePatterns = values(appCodePatterns, false);
        envPatterns = values(envPatterns, false);
        bizCodePatterns = values(bizCodePatterns, false);
        allowedOperations = values(allowedOperations, true);
    }

    public boolean permits(
            String expectedClientType,
            String operation,
            String appCode,
            String env,
            String bizCode) {
        return ("*".equals(clientType)
                || clientType.equalsIgnoreCase(expectedClientType))
                && (allowedOperations.contains("*")
                || allowedOperations.contains(operation.toUpperCase(
                        Locale.ROOT
                )))
                && matches(appCodePatterns, appCode)
                && matches(envPatterns, env)
                && matches(bizCodePatterns, bizCode);
    }

    public DdcServicePrincipal principal(
            String appCode,
            String env,
            String bizCode) {
        return new DdcServicePrincipal(
                credentialId,
                clientType,
                appCodePatterns,
                envPatterns,
                bizCodePatterns,
                allowedOperations,
                appCode,
                env,
                bizCode
        );
    }

    /** 返回不暴露 HMAC Secret 的诊断文本。 / Returns diagnostic text without exposing the HMAC secret. */
    @Override
    public String toString() {
        return "DdcHmacCredential[credentialId=" + credentialId
                + ", accessKey=" + accessKey
                + ", secret=<redacted>, clientType=" + clientType
                + ", appCodePatterns=" + appCodePatterns
                + ", envPatterns=" + envPatterns
                + ", bizCodePatterns=" + bizCodePatterns
                + ", allowedOperations=" + allowedOperations + ']';
    }

    private boolean matches(Set<String> patterns, String value) {
        if (value == null) {
            return true;
        }
        return patterns.stream().anyMatch(pattern ->
                PatternMatchUtils.simpleMatch(pattern, value)
        );
    }

    private static Set<String> values(
            Collection<String> source,
            boolean uppercase) {
        if (source == null || source.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        source.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .map(value -> uppercase
                        ? value.toUpperCase(Locale.ROOT)
                        : value)
                .forEach(result::add);
        return Set.copyOf(result);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
