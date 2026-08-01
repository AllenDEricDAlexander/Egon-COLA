package top.egon.cola.platform.rbac3.core.hierarchy;

public record RoleNode(
        String id,
        String applicationId,
        String code,
        boolean active,
        RiskLevel riskLevel,
        boolean privileged,
        String landingRouteCode,
        int landingPriority
) {

    public RoleNode {
        id = required(id, "id");
        applicationId = required(applicationId, "applicationId");
        code = required(code, "code");
        if (riskLevel == null) {
            throw new IllegalArgumentException("riskLevel is required");
        }
        if (landingPriority < 0) {
            throw new IllegalArgumentException("landingPriority must not be negative");
        }
        if (landingRouteCode != null) {
            landingRouteCode = required(landingRouteCode, "landingRouteCode");
        }
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
