package top.egon.cola.component.common.mybatis.business;

import org.slf4j.MDC;

/**
 * Static read access to the trusted tenant identifier carried by the configured MDC key.
 *
 * <p>The class is an inconstructible utility: {@link #initialize(String)} stores only the MDC key
 * name, never a tenant value, and {@link #currentTenantId()} re-reads SLF4J on every call so a
 * pooled thread can never observe another request's tenant. Positive-value and system-zero
 * policies stay with the guards that already enforce them.</p>
 */
public final class EgonColaTenantIdProvider {

    /** MDC key used when no explicit key is configured. */
    public static final String DEFAULT_MDC_KEY = "tenantId";

    private static volatile String mdcKey = DEFAULT_MDC_KEY;

    private EgonColaTenantIdProvider() {
    }

    /**
     * Publishes the configured MDC key name.
     *
     * @param configuredMdcKey non-blank MDC key that carries the tenant identifier
     */
    public static void initialize(String configuredMdcKey) {
        if (configuredMdcKey == null || configuredMdcKey.isBlank()) {
            throw new IllegalArgumentException(
                    "egon.cola.component.mybatis-plus.tenant-id.mdc-key must not be blank");
        }
        mdcKey = configuredMdcKey;
    }

    /**
     * Returns the tenant identifier of the calling thread.
     *
     * @return the parsed tenant identifier
     * @throws IllegalStateException when the context is missing or is not a long value
     */
    public static Long currentTenantId() {
        String value = MDC.get(mdcKey);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("TENANT_CONTEXT_MISSING");
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("TENANT_CONTEXT_MALFORMED", exception);
        }
    }
}
