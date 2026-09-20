package top.egon.cola.component.common.mybatis.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaUserIdProvider;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Authoritative fill template for EgonModel technical fields.
 */
@Slf4j
@RequiredArgsConstructor
public class EgonColaMetaObjectHandler implements MetaObjectHandler {

    @Getter
    @Qualifier("egonColaMdcUserIdProvider")
    private final EgonColaUserIdProvider userIdProvider;

    @Qualifier("egonColaMybatisPlusClock")
    private final Clock clock;

    @Override
    public final void insertFill(MetaObject metaObject) {
        if (!(metaObject.getOriginalObject() instanceof EgonModel<?>)) {
            return;
        }
        Long tenantId = requireTenantId();
        String userId = requireUserId();
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        metaObject.setValue("tenantId", tenantId);
        metaObject.setValue("createUserId", userId);
        metaObject.setValue("createTime", now);
        metaObject.setValue("updateUserId", userId);
        metaObject.setValue("updateTime", now);
        metaObject.setValue("deletedAt", null);
        metaObject.setValue("version", 0L);
        Object[] technical = technicalFields(metaObject);
        afterInsertFill(metaObject);
        verifyTechnicalFields(metaObject, technical);
    }

    @Override
    public final void updateFill(MetaObject metaObject) {
        if (!(metaObject.getOriginalObject() instanceof EgonModel<?>)) {
            return;
        }
        Long tenantId = requireTenantId();
        Object previousTenant = metaObject.getValue("tenantId");
        if (previousTenant != null && !tenantId.equals(previousTenant)) {
            throw new IllegalStateException("TENANT_CONTEXT_MISMATCH");
        }
        metaObject.setValue("tenantId", tenantId);
        metaObject.setValue("updateUserId", requireUserId());
        metaObject.setValue("updateTime", clock.instant().truncatedTo(ChronoUnit.MICROS));
        Object[] technical = technicalFields(metaObject);
        afterUpdateFill(metaObject);
        verifyTechnicalFields(metaObject, technical);
    }

    private static Object[] technicalFields(MetaObject metaObject) {
        return new Object[]{metaObject.getValue("id"), metaObject.getValue("tenantId"),
                metaObject.getValue("createUserId"), metaObject.getValue("createTime"), metaObject.getValue("updateUserId"),
                metaObject.getValue("updateTime"), metaObject.getValue("deletedAt"), metaObject.getValue("version")};
    }

    private static void verifyTechnicalFields(MetaObject metaObject, Object[] expected) {
        if (!java.util.Arrays.equals(expected, technicalFields(metaObject))) {
            throw new IllegalStateException("TECHNICAL_FIELD_OVERRIDE_FORBIDDEN");
        }
    }

    protected void afterInsertFill(MetaObject metaObject) {
    }

    protected void afterUpdateFill(MetaObject metaObject) {
    }

    private Long requireTenantId() {
        return EgonColaTenantIdProvider.currentTenantId();
    }

    private String requireUserId() {
        String userId = userIdProvider.currentUserId();
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("USER_CONTEXT_MISSING");
        }
        return userId;
    }
}
