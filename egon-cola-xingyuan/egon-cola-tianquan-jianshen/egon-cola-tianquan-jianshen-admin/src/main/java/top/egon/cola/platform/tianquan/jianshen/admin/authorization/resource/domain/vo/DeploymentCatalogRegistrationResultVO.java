package top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.vo;

/** Immutable committed mechanical catalog receipt; pending review never implies resource access. */
public record DeploymentCatalogRegistrationResultVO(
        String applicationId, String applicationCode, String buildId, String checksum,
        long applicationVersion, int permissionCount, int resourceCount, int fieldCount, String status) {
    public DeploymentCatalogRegistrationResultVO {
        if (applicationId == null || !applicationId.matches("[1-9][0-9]{0,18}")
                || applicationCode == null || !applicationCode.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")
                || buildId == null || buildId.isBlank() || buildId.length() > 256
                || checksum == null || !checksum.matches("[0-9a-f]{64}") || applicationVersion < 0
                || permissionCount < 0 || permissionCount > 64 || resourceCount < 0 || resourceCount > 128
                || fieldCount < 0 || fieldCount > 256 || !"REGISTERED_PENDING_REVIEW".equals(status)) {
            throw new IllegalArgumentException("Deployment catalog receipt is invalid");
        }
        try { Long.parseLong(applicationId); }
        catch (NumberFormatException invalid) { throw new IllegalArgumentException("Deployment catalog application ID is invalid", invalid); }
    }
}
