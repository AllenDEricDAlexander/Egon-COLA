package top.egon.cola.component.common.mybatis.business;

import org.slf4j.MDC;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;

import java.util.Objects;

/**
 * Default tenant provider backed by the configured MDC key.
 */
public final class EgonColaMdcTenantIdProvider implements EgonColaTenantIdProvider {

    private final String mdcKey;

    public EgonColaMdcTenantIdProvider(EgonColaMybatisPlusProperties properties) {
        this.mdcKey = Objects.requireNonNull(properties, "properties must not be null")
                .getTenantId().getMdcKey();
    }

    @Override
    public Long currentTenantId() {
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
