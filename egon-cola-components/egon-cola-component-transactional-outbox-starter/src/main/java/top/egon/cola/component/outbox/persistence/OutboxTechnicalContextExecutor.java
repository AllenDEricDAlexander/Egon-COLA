package top.egon.cola.component.outbox.persistence;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;

import java.util.function.Supplier;

/** Applies the framework owner and actor only while synchronous MP operations validate and execute. */
@Slf4j
@Validated
@RequiredArgsConstructor
public class OutboxTechnicalContextExecutor {

    private static final String TECHNICAL_TENANT_ID = "0";
    private static final String TECHNICAL_USER_ID = "system:outbox";

    @Qualifier("egon.cola.component.mybatis-plus-top.top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    public <T> T execute(@NotNull Supplier<T> action) {
        if (action == null) {
            throw new IllegalArgumentException("OUTBOX_TECHNICAL_ACTION_REQUIRED");
        }
        String tenantMdcKey = properties.getTenantId().getMdcKey();
        String userIdMdcKey = properties.getAudit().getUserIdMdcKey();
        if (tenantMdcKey.equals(userIdMdcKey)) {
            throw new IllegalStateException("OUTBOX_TECHNICAL_MDC_KEYS_MUST_DIFFER");
        }

        String previousTenantId = MDC.get(tenantMdcKey);
        String previousUserId = MDC.get(userIdMdcKey);
        try {
            MDC.put(tenantMdcKey, TECHNICAL_TENANT_ID);
            MDC.put(userIdMdcKey, TECHNICAL_USER_ID);
            return action.get();
        } finally {
            restore(userIdMdcKey, previousUserId);
            restore(tenantMdcKey, previousTenantId);
        }
    }

    private static void restore(String key, String previousValue) {
        if (previousValue == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, previousValue);
        }
    }
}
