package top.egon.cola.component.common.mybatis.support;

import org.slf4j.MDC;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;

/**
 * Mutable test-only tenant context fixture.
 *
 * <p>Tenant access is static and MDC backed, so the fixture publishes the value the same way a real
 * request boundary does instead of being injected into a collaborator.</p>
 */
public final class TestTenantIdProvider {

    public void set(Long tenantId) {
        if (tenantId == null) {
            clear();
            return;
        }
        MDC.put(EgonColaTenantIdProvider.DEFAULT_MDC_KEY, String.valueOf(tenantId));
    }

    public void clear() {
        MDC.remove(EgonColaTenantIdProvider.DEFAULT_MDC_KEY);
    }
}
