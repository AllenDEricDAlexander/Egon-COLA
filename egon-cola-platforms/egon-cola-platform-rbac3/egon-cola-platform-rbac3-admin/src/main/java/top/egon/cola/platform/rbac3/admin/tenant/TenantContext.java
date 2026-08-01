package top.egon.cola.platform.rbac3.admin.tenant;

public record TenantContext(
        String authenticatedTenantId,
        String effectiveTenantId,
        boolean platformTarget
) {

    private static final ThreadLocal<TenantContext> CURRENT = new ThreadLocal<>();

    public static void set(TenantContext context) {
        CURRENT.set(context);
    }

    public static TenantContext requireCurrent() {
        TenantContext context = CURRENT.get();
        if (context == null) {
            throw new IllegalStateException("tenant context is not available");
        }
        return context;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
