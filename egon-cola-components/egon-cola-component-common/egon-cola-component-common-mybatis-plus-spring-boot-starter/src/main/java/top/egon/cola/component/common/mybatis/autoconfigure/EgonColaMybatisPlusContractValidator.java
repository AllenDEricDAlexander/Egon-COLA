package top.egon.cola.component.common.mybatis.autoconfigure;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.handler.EgonColaMetaObjectHandler;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaModelValidationInterceptor;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaTenantIdGuardInnerInterceptor;

import java.util.List;
import java.util.Objects;

/**
 * Verifies the final runtime chain after all consumer beans have been
 * registered. This prevents conditional backoff from silently removing a
 * mandatory isolation, fill, or validation capability.
 */
public final class EgonColaMybatisPlusContractValidator implements SmartInitializingSingleton {

    private final ObjectProvider<MybatisPlusInterceptor> outerProvider;
    private final ObjectProvider<MetaObjectHandler> handlerProvider;
    private final ObjectProvider<EgonColaModelValidationInterceptor> validationProvider;
    private final EgonColaMybatisPlusProperties properties;

    public EgonColaMybatisPlusContractValidator(
            ObjectProvider<MybatisPlusInterceptor> outerProvider,
            ObjectProvider<MetaObjectHandler> handlerProvider,
            ObjectProvider<EgonColaModelValidationInterceptor> validationProvider,
            EgonColaMybatisPlusProperties properties) {
        this.outerProvider = Objects.requireNonNull(outerProvider, "outerProvider must not be null");
        this.handlerProvider = Objects.requireNonNull(handlerProvider, "handlerProvider must not be null");
        this.validationProvider = Objects.requireNonNull(validationProvider,
                "validationProvider must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public void afterSingletonsInstantiated() {
        MybatisPlusInterceptor outer = outerProvider.getIfAvailable();
        if (outer == null) {
            throw failure("MYBATIS_PLUS_OUTER_INTERCEPTOR_MISSING");
        }
        validateInnerChain(outer.getInterceptors());
        if (validationProvider.getIfAvailable() == null) {
            throw failure("MODEL_VALIDATION_INTERCEPTOR_MISSING");
        }
        List<MetaObjectHandler> handlers = handlerProvider.orderedStream().toList();
        if (handlers.size() != 1 || !(handlers.get(0) instanceof EgonColaMetaObjectHandler)) {
            throw failure("META_OBJECT_HANDLER_CONTRACT_INVALID");
        }
    }

    private void validateInnerChain(List<InnerInterceptor> chain) {
        if (chain == null || chain.isEmpty()) {
            throw failure("MYBATIS_PLUS_INTERCEPTOR_ORDER_INVALID");
        }
        int guard = uniqueIndex(chain, EgonColaTenantIdGuardInnerInterceptor.class);
        int tenantLine = uniqueIndex(chain, TenantLineInnerInterceptor.class);
        if (guard < 0 || tenantLine < 0 || guard > tenantLine) {
            throw failure("MYBATIS_PLUS_INTERCEPTOR_ORDER_INVALID");
        }
        int blockAttack = uniqueIndex(chain, BlockAttackInnerInterceptor.class);
        if (blockAttack == -2 || blockAttack >= 0 && (blockAttack < guard || blockAttack > tenantLine)) {
            throw failure("MYBATIS_PLUS_INTERCEPTOR_ORDER_INVALID");
        }
        int optimistic = uniqueIndex(chain, OptimisticLockerInnerInterceptor.class);
        int pagination = uniqueIndex(chain, PaginationInnerInterceptor.class);
        if (optimistic == -2 || pagination == -2
                || optimistic >= 0 && pagination >= 0 && optimistic > pagination) {
            throw failure("MYBATIS_PLUS_INTERCEPTOR_ORDER_INVALID");
        }
        if (properties.getPagination().isEnabled() && pagination < 0) {
            throw failure("MYBATIS_PLUS_INTERCEPTOR_ORDER_INVALID");
        }
        if (properties.getOptimisticLocker().isEnabled() && optimistic < 0) {
            throw failure("MYBATIS_PLUS_INTERCEPTOR_ORDER_INVALID");
        }
        if (properties.getBlockAttack().isEnabled() && blockAttack < 0) {
            throw failure("MYBATIS_PLUS_INTERCEPTOR_ORDER_INVALID");
        }
    }

    private static int uniqueIndex(List<InnerInterceptor> chain,
                                   Class<? extends InnerInterceptor> type) {
        int found = -1;
        for (int index = 0; index < chain.size(); index++) {
            if (type.isInstance(chain.get(index))) {
                if (found >= 0) {
                    return -2;
                }
                found = index;
            }
        }
        return found;
    }

    private static EgonColaMybatisPlusConfigurationException failure(String code) {
        return new EgonColaMybatisPlusConfigurationException(code);
    }
}
