package top.egon.cola.component.common.mybatis.support;

import top.egon.cola.component.common.mybatis.business.EgonColaUserIdProvider;

/**
 * Mutable test-only user context fixture.
 */
public final class TestUserIdProvider implements EgonColaUserIdProvider {

    private final ThreadLocal<String> current = new ThreadLocal<>();
    private int reads;

    public void set(String userId) {
        current.set(userId);
    }

    public void clear() {
        current.remove();
    }

    @Override
    public String currentUserId() {
        reads++;
        return current.get();
    }

    public int reads() {
        return reads;
    }
}
