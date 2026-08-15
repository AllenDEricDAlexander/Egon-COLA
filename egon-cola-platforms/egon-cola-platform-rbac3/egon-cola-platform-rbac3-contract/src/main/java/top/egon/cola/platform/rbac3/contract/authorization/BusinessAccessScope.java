package top.egon.cola.platform.rbac3.contract.authorization;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Minimal DDC Business identity and its authorized Application children.
 */
public record BusinessAccessScope(
        String businessId,
        String businessCode,
        List<ApplicationAccessScope> applications
) {

    public BusinessAccessScope {
        businessId = required(businessId, "businessId");
        businessCode = required(businessCode, "businessCode");
        applications = Objects.requireNonNull(applications, "applications").stream()
                .map(application -> Objects.requireNonNull(
                        application, "applications must not contain null"))
                .sorted(Comparator.comparing(ApplicationAccessScope::applicationCode)
                        .thenComparing(ApplicationAccessScope::applicationId))
                .toList();
        var applicationCodes = new HashSet<String>();
        for (ApplicationAccessScope application : applications) {
            if (!applicationCodes.add(application.applicationCode())) {
                throw new IllegalArgumentException(
                        "applications must have unique applicationCode values");
            }
        }
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
