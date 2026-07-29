package top.egon.cola.component.accessguard.store.redisson;

import java.util.Objects;
import java.util.regex.Pattern;

public final class AccessGuardRedisKeyFactory {

    private static final Pattern SEGMENT = Pattern.compile("[A-Za-z0-9._-]+");
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");

    private final String prefix;
    private final String application;

    public AccessGuardRedisKeyFactory(String prefix, String application) {
        this.prefix = requirePrefix(prefix);
        this.application = requireSegment(application, "application");
    }

    public String allowList(String ruleId, String dataVersion) {
        return list(ruleId, "allow-list", dataVersion);
    }

    public String denyList(String ruleId, String dataVersion) {
        return list(ruleId, "deny-list", dataVersion);
    }

    public String penalty(String ruleId, String stateVersion, String keyHash) {
        return state(ruleId, "penalty-box", stateVersion, keyHash);
    }

    public String rateLimit(String ruleId, String stateVersion, String keyHash) {
        return state(ruleId, "rate-limit", stateVersion, keyHash);
    }

    private String list(String ruleId, String policy, String dataVersion) {
        return prefix + ':' + application + ':'
                + requireSegment(ruleId, "ruleId") + ':' + policy + ':'
                + requireSegment(dataVersion, "dataVersion");
    }

    private String state(String ruleId, String policy, String stateVersion, String keyHash) {
        String hash = Objects.requireNonNull(keyHash, "keyHash");
        if (!HASH.matcher(hash).matches()) {
            throw new IllegalArgumentException("keyHash must be a lowercase SHA-256 value");
        }
        return prefix + ':' + application + ':'
                + requireSegment(ruleId, "ruleId") + ':' + policy + ':'
                + requireSegment(stateVersion, "stateVersion") + ':' + hash;
    }

    private static String requirePrefix(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("keyPrefix must not be blank");
        }
        String normalized = value.trim();
        if (normalized.startsWith(":") || normalized.endsWith(":") || normalized.contains("::")) {
            throw new IllegalArgumentException("keyPrefix must contain non-empty segments");
        }
        for (String segment : normalized.split(":")) {
            requireSegment(segment, "keyPrefix");
        }
        return normalized;
    }

    private static String requireSegment(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        String normalized = value.trim();
        if (!SEGMENT.matcher(normalized).matches()) {
            throw new IllegalArgumentException(name + " contains unsafe characters");
        }
        return normalized;
    }
}
