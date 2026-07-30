package top.egon.cola.platform.rbac3.admin.tenant;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3Principal;

public final class TenantContextResolver {

    public static final String TARGET_HEADER = "X-RBAC3-Target-Tenant";
    public static final String TENANT_HEADER = "X-RBAC3-Tenant";
    private static final String TARGET_PERMISSION = "system:tenant:target";

    public TenantContext resolve(HttpServletRequest request, Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof CurrentRbac3Principal principal)) {
            throw new TenantContextResolutionException(401, "AUTHENTICATION_REQUIRED");
        }
        String assertedTenant = trimToNull(request.getHeader(TENANT_HEADER));
        if (assertedTenant != null && !assertedTenant.equals(principal.tenantId())) {
            throw new TenantContextResolutionException(400, "TENANT_CONTEXT_INVALID");
        }

        String targetTenant = trimToNull(request.getHeader(TARGET_HEADER));
        if (targetTenant == null) {
            return new TenantContext(principal.tenantId(), principal.tenantId(), false);
        }
        boolean platformRoute = request.getRequestURI().startsWith("/api/rbac3/v1/platform/")
                || request.getRequestURI().startsWith("/api/v1/platform/");
        if (!platformRoute || !principal.platformAdministrator()
                || !principal.hasPermission(TARGET_PERMISSION)) {
            throw new TenantContextResolutionException(403, "PERMISSION_DENIED");
        }
        return new TenantContext(principal.tenantId(), targetTenant, true);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static final class TenantContextResolutionException extends RuntimeException {
        private final int status;
        private final String reasonCode;

        public TenantContextResolutionException(int status, String reasonCode) {
            super(reasonCode);
            this.status = status;
            this.reasonCode = reasonCode;
        }

        public int status() {
            return status;
        }

        public String reasonCode() {
            return reasonCode;
        }
    }
}
