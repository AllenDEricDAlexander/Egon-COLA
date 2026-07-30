package top.egon.cola.platform.rbac3.core.runtime;

import java.util.regex.Pattern;

public final class Rbac3RuntimeKeyFactory {

    private static final Pattern SEGMENT = Pattern.compile("[A-Za-z0-9._-]{1,128}");

    public String session(String tenantId, String sessionId) {
        return prefix(tenantId) + "session:" + segment(sessionId, "sessionId");
    }

    public String authVersion(String tenantId, String userId) {
        return prefix(tenantId) + "auth-version:" + segment(userId, "userId");
    }

    public String policyVersion(String tenantId) {
        return prefix(tenantId) + "policy-version";
    }

    public String snapshot(String tenantId, String sessionId, long sessionVersion) {
        if (sessionVersion < 0) {
            throw new IllegalArgumentException("sessionVersion must not be negative");
        }
        return prefix(tenantId) + "snapshot:" + segment(sessionId, "sessionId")
                + ':' + sessionVersion;
    }

    public String sessionFence(String tenantId, String sessionId) {
        return prefix(tenantId) + "fence:session:" + segment(sessionId, "sessionId");
    }

    public String operationMapping(
            String tenantId,
            String applicationCode,
            long mappingVersion
    ) {
        if (mappingVersion < 0) {
            throw new IllegalArgumentException("mappingVersion must not be negative");
        }
        return prefix(tenantId) + "operation-mapping:"
                + segment(applicationCode, "applicationCode") + ':' + mappingVersion;
    }

    public String keyRing(String tenantId) {
        return prefix(tenantId) + "key-ring";
    }

    private String prefix(String tenantId) {
        return "rbac3:{" + segment(tenantId, "tenantId") + "}:";
    }

    private String segment(String value, String field) {
        if (value == null || !SEGMENT.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " is not a safe Redis key segment");
        }
        return value;
    }
}
