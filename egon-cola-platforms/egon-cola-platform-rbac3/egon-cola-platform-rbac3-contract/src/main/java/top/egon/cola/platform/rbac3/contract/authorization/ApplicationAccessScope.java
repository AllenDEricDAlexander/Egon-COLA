package top.egon.cola.platform.rbac3.contract.authorization;

/**
 * Minimal DDC Application identity exposed to the Gateway authorization layer.
 */
public record ApplicationAccessScope(
        String applicationId,
        String applicationCode
) {

    public ApplicationAccessScope {
        applicationId = required(applicationId, "applicationId");
        applicationCode = required(applicationCode, "applicationCode");
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
