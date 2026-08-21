package top.egon.cola.component.common.mybatis.business;

import org.slf4j.MDC;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;

import java.util.Objects;

/**
 * Default audit user provider backed by the configured MDC key.
 */
public final class EgonColaMdcUserIdProvider implements EgonColaUserIdProvider {

    private final String mdcKey;

    public EgonColaMdcUserIdProvider(EgonColaMybatisPlusProperties properties) {
        this.mdcKey = Objects.requireNonNull(properties, "properties must not be null")
                .getAudit().getUserIdMdcKey();
    }

    @Override
    public String currentUserId() {
        String value = MDC.get(mdcKey);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("USER_CONTEXT_MISSING");
        }
        return value.trim();
    }
}
