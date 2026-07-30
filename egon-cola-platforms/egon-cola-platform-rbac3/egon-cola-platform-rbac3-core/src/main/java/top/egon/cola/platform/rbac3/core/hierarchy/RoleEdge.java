package top.egon.cola.platform.rbac3.core.hierarchy;

public record RoleEdge(String seniorRoleId, String juniorRoleId) {

    public RoleEdge {
        seniorRoleId = required(seniorRoleId, "seniorRoleId");
        juniorRoleId = required(juniorRoleId, "juniorRoleId");
        if (seniorRoleId.equals(juniorRoleId)) {
            throw new IllegalArgumentException("role inheritance must be distinct");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
