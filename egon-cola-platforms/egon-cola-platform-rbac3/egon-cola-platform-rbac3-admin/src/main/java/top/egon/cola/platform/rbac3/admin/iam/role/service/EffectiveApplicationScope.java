package top.egon.cola.platform.rbac3.admin.iam.role.service;

/**
 * DDC Business and Application identity after all local and catalog eligibility checks.
 */
public record EffectiveApplicationScope(
        String businessId,
        String businessCode,
        String applicationId,
        String applicationCode
) {

    public EffectiveApplicationScope {
        businessId = required(businessId, "businessId");
        businessCode = required(businessCode, "businessCode");
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
