package top.egon.cola.component.common.mybatis.support;

import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;

/**
 * Mutable test-only tenant context fixture.
 */
public final class TestTenantIdProvider implements EgonColaTenantIdProvider {

    private final ThreadLocal<Long> current = new ThreadLocal<>();
    private int reads;

    public void set(Long tenantId) {
        current.set(tenantId);
    }

    public void clear() {
        current.remove();
    }

    @Override
    public Long currentTenantId() {
        reads++;
        return current.get();
    }

    public int reads() {
        return reads;
    }
}
