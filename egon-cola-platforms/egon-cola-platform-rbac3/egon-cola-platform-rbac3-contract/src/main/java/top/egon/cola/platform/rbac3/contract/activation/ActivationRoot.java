package top.egon.cola.platform.rbac3.contract.activation;

public record ActivationRoot(
        String roleId,
        String applicationId,
        String roleCode
) {

    public ActivationRoot {
        roleId = required(roleId, "roleId");
        applicationId = required(applicationId, "applicationId");
        roleCode = required(roleCode, "roleCode");
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
