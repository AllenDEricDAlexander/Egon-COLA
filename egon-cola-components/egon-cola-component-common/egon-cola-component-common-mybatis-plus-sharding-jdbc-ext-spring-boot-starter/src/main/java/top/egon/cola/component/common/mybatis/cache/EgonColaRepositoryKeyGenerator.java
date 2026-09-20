package top.egon.cola.component.common.mybatis.cache;

import org.springframework.cache.interceptor.KeyGenerator;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * 仓储缓存键策略（Strategy，MC-PATTERN-001）：把 {@code tenant:id} 的键形状收口到唯一入口，
 * 替代逐处 SpEL 拼接。租户恒取调用线程的可信上下文，方法名不进入键，
 * 因此读、改、删三个入口天然共用同一区域键。批量失效不走本策略，
 * 由 {@code EgonColaCachePort} 的显式精确键集合承担。
 */
public class EgonColaRepositoryKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        if (params == null || params.length != 1) {
            throw new IllegalArgumentException("CACHE_KEY_ARGUMENT_UNSUPPORTED: "
                    + (params == null ? 0 : params.length));
        }
        Long tenantId = EgonColaTenantIdProvider.currentTenantId();
        long id = requireId(params[0]);
        return tenantId + ":" + id;
    }

    private static long requireId(Object param) {
        if (param instanceof Long id) {
            return requirePositive(id);
        }
        if (param instanceof EgonModel<?> model) {
            Long own = model.getTenantId();
            if (own != null && !own.equals(EgonColaTenantIdProvider.currentTenantId())) {
                throw new IllegalArgumentException("CACHE_KEY_TENANT_MISMATCH: " + own);
            }
            return requirePositive(model.getId());
        }
        // 批量签名（集合/数组）无法产出单一可信键，属于签名不支持而不是非法 ID
        if (param instanceof Collection<?> || (param != null && param.getClass().isArray())) {
            throw new IllegalArgumentException("CACHE_KEY_ARGUMENT_UNSUPPORTED: "
                    + param.getClass().getSimpleName());
        }
        throw new IllegalArgumentException("CACHE_KEY_ID_INVALID: "
                + (param == null ? "null" : param.getClass().getSimpleName()));
    }

    private static long requirePositive(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("CACHE_KEY_ID_INVALID: " + id);
        }
        return id;
    }
}
