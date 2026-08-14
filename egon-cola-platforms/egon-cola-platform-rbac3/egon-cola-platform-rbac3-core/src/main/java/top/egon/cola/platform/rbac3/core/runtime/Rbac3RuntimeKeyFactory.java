package top.egon.cola.platform.rbac3.core.runtime;

import java.util.regex.Pattern;

public final class Rbac3RuntimeKeyFactory {

    private static final Pattern SEGMENT = Pattern.compile("[A-Za-z0-9._-]{1,128}");

    public String authVersion(String tenantId, String userId) {
        return prefix(tenantId) + "auth-version:" + segment(userId, "userId");
    }

    /**
     * Current publication pointer for one IdP subject.
     */
    public String user(String tenantId, String identitySub) {
        return prefix(tenantId) + "user:" + segment(identitySub, "identitySub");
    }

    public String policyVersion(String tenantId) {
        return prefix(tenantId) + "policy-version";
    }

    public String snapshot(String tenantId, String identitySub, long authVersion) {
        if (authVersion < 0) {
            throw new IllegalArgumentException("authVersion must not be negative");
        }
        return prefix(tenantId) + "snapshot:" + segment(identitySub, "identitySub")
                + ':' + authVersion;
    }

    public String authorizationPublicationGuard(String tenantId, String identitySub) {
        return prefix(tenantId) + "publication-guard:user:" + segment(identitySub, "identitySub");
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

    public String operationMapping(
            String tenantId,
            String definitionSetId,
            String gatewayOperationId,
            long publishedVersion
    ) {
        if (publishedVersion < 0) {
            throw new IllegalArgumentException(
                    "publishedVersion must not be negative");
        }
        return prefix(tenantId) + "operation-mapping:"
                + segment(definitionSetId, "definitionSetId") + ':'
                + segment(gatewayOperationId, "gatewayOperationId") + ':'
                + publishedVersion;
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
