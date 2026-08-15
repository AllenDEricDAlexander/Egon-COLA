package top.egon.cola.platform.rbac3.contract.authorization;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Immutable USER authorization projection containing only Gateway BIZ and APP scope.
 */
public record GatewayBizAppScopeSnapshot(
        String tenantId,
        String identitySub,
        String rbacUserId,
        long authVersion,
        long policyVersion,
        List<BusinessAccessScope> businesses,
        String checksum,
        Instant generatedAt,
        Instant expiresAt
) {

    public GatewayBizAppScopeSnapshot {
        tenantId = required(tenantId, "tenantId");
        identitySub = required(identitySub, "identitySub");
        rbacUserId = required(rbacUserId, "rbacUserId");
        nonNegative(authVersion, "authVersion");
        nonNegative(policyVersion, "policyVersion");
        businesses = Objects.requireNonNull(businesses, "businesses").stream()
                .map(business -> Objects.requireNonNull(
                        business, "businesses must not contain null"))
                .sorted(Comparator.comparing(BusinessAccessScope::businessCode)
                        .thenComparing(BusinessAccessScope::businessId))
                .toList();
        var businessCodes = new HashSet<String>();
        for (BusinessAccessScope business : businesses) {
            if (!businessCodes.add(business.businessCode())) {
                throw new IllegalArgumentException(
                        "businesses must have unique businessCode values");
            }
        }
        checksum = required(checksum, "checksum");
        generatedAt = Objects.requireNonNull(generatedAt, "generatedAt");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(generatedAt)) {
            throw new IllegalArgumentException("expiresAt must be after generatedAt");
        }
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static void nonNegative(long value, String fieldName) {
        if (value < 0L) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
    }
}
