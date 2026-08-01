package top.egon.cola.platform.rbac3.core.activation;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public record ActiveRoleSet(
        String tenantId,
        String userId,
        String sessionId,
        Map<String, Set<String>> rootsByApplication,
        String checksum
) {

    public ActiveRoleSet {
        tenantId = required(tenantId, "tenantId");
        userId = required(userId, "userId");
        sessionId = required(sessionId, "sessionId");
        checksum = required(checksum, "checksum");
        var normalized = new TreeMap<String, Set<String>>();
        rootsByApplication.forEach((applicationId, rootIds) -> normalized.put(
                required(applicationId, "applicationId"),
                Collections.unmodifiableSet(new TreeSet<>(rootIds))
        ));
        rootsByApplication = Collections.unmodifiableMap(normalized);
    }

    public Set<String> rootIds() {
        var result = new TreeSet<String>();
        rootsByApplication.values().forEach(result::addAll);
        return Collections.unmodifiableSet(result);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
