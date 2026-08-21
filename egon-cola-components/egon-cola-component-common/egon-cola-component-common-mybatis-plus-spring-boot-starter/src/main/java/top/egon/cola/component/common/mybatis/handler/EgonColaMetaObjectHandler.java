package top.egon.cola.component.common.mybatis.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaUserIdProvider;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Authoritative fill template for EgonModel technical fields.
 */
public class EgonColaMetaObjectHandler implements MetaObjectHandler {

    private final EgonColaTenantIdProvider tenantIdProvider;
    private final EgonColaUserIdProvider userIdProvider;
    private final Clock clock;

    public EgonColaMetaObjectHandler(EgonColaTenantIdProvider tenantIdProvider,
                                     EgonColaUserIdProvider userIdProvider,
                                     Clock clock) {
        this.tenantIdProvider = Objects.requireNonNull(tenantIdProvider, "tenantIdProvider must not be null");
        this.userIdProvider = Objects.requireNonNull(userIdProvider, "userIdProvider must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public final void insertFill(MetaObject metaObject) {
        if (!(metaObject.getOriginalObject() instanceof EgonModel<?>)) {
            return;
        }
        Long tenantId = requireTenantId();
        String userId = requireUserId();
        Instant now = clock.instant();
        metaObject.setValue("tenantId", tenantId);
        metaObject.setValue("createUserId", userId);
        metaObject.setValue("createTime", now);
        metaObject.setValue("updateUserId", userId);
        metaObject.setValue("updateTime", now);
        metaObject.setValue("isDeleted", false);
        afterInsertFill(metaObject);
    }

    @Override
    public final void updateFill(MetaObject metaObject) {
        if (!(metaObject.getOriginalObject() instanceof EgonModel<?>)) {
            return;
        }
        metaObject.setValue("tenantId", requireTenantId());
        metaObject.setValue("updateUserId", requireUserId());
        metaObject.setValue("updateTime", clock.instant());
        afterUpdateFill(metaObject);
    }

    protected void afterInsertFill(MetaObject metaObject) {
    }

    protected void afterUpdateFill(MetaObject metaObject) {
    }

    private Long requireTenantId() {
        Long tenantId = tenantIdProvider.currentTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("TENANT_CONTEXT_MISSING");
        }
        return tenantId;
    }

    private String requireUserId() {
        String userId = userIdProvider.currentUserId();
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("USER_CONTEXT_MISSING");
        }
        return userId;
    }
}
