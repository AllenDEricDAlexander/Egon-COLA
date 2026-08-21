package top.egon.cola.component.common.mybatis.model;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaMdcTenantIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaMdcUserIdProvider;
import top.egon.cola.component.common.mybatis.handler.EgonColaMetaObjectHandler;
import top.egon.cola.component.common.mybatis.support.TestBusinessModel;
import top.egon.cola.component.common.mybatis.support.TestTenantIdProvider;
import top.egon.cola.component.common.mybatis.support.TestUserIdProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EgonModelTest {

    private static final Instant NOW = Instant.parse("2026-08-21T06:00:00Z");

    @AfterEach
    void clearContext() {
        MDC.clear();
    }

    @Test
    void propertiesHaveSafeTechnicalDefaults() {
        EgonColaMybatisPlusProperties properties = new EgonColaMybatisPlusProperties();

        assertTrue(properties.isEnabled());
        assertEquals("tenantId", properties.getTenantId().getMdcKey());
        assertTrue(properties.getTenantId().getIgnoredTables().isEmpty());
        assertEquals("userId", properties.getAudit().getUserIdMdcKey());
        assertTrue(properties.getPagination().isEnabled());
        assertEquals(500, properties.getPagination().getMaxPageSize());
        assertFalse(properties.getPagination().isOverflow());
        assertEquals(1_000, properties.getBatch().getDefaultSize());
        assertEquals(1_000, properties.getBatch().getMaxChunkSize());
        assertEquals(10_000, properties.getBatch().getMaxCollectionSize());
        assertTrue(properties.getBlockAttack().isEnabled());
        assertTrue(properties.getOptimisticLocker().isEnabled());
        assertTrue(properties.getMetaFill().isEnabled());
    }

    @Test
    void mdcTenantProviderAcceptsAnyLongAndFailsClosed() {
        EgonColaMybatisPlusProperties properties = new EgonColaMybatisPlusProperties();
        EgonColaMdcTenantIdProvider provider = new EgonColaMdcTenantIdProvider(properties);

        MDC.put("tenantId", "0");
        assertEquals(0L, provider.currentTenantId());
        MDC.put("tenantId", "-7");
        assertEquals(-7L, provider.currentTenantId());
        MDC.put("tenantId", String.valueOf(Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, provider.currentTenantId());
        MDC.remove("tenantId");
        assertContextFailure("TENANT_CONTEXT_MISSING", provider::currentTenantId);
        MDC.put("tenantId", "not-a-long");
        assertContextFailure("TENANT_CONTEXT_MALFORMED", provider::currentTenantId);
    }

    @Test
    void mdcUserProviderReturnsOnlyTrimmedIdAndFailsClosed() {
        EgonColaMdcUserIdProvider provider = new EgonColaMdcUserIdProvider(
                new EgonColaMybatisPlusProperties());

        MDC.put("userId", " operator-7 ");
        assertEquals("operator-7", provider.currentUserId());
        MDC.put("userId", " ");
        assertContextFailure("USER_CONTEXT_MISSING", provider::currentUserId);
        MDC.remove("userId");
        assertContextFailure("USER_CONTEXT_MISSING", provider::currentUserId);
    }

    @Test
    void insertFillAuthoritativelyOverwritesTechnicalFieldsAndUsesOneInstant() {
        TestTenantIdProvider tenant = new TestTenantIdProvider();
        tenant.set(-7L);
        TestUserIdProvider user = new TestUserIdProvider();
        user.set("operator-7");
        EgonColaMetaObjectHandler handler = new EgonColaMetaObjectHandler(
                tenant, user, Clock.fixed(NOW, ZoneOffset.UTC));
        TestBusinessModel model = new TestBusinessModel().businessValues("title", "payload");
        model.setId(99L);
        model.setTenantId(42L);
        model.setCreateUserId("forged-create");
        model.setCreateTime(NOW.minusSeconds(1));
        model.setUpdateUserId("forged-update");
        model.setUpdateTime(NOW.minusSeconds(1));
        model.setIsDeleted(true);

        handler.insertFill(SystemMetaObject.forObject(model));

        assertEquals(99L, model.getId());
        assertEquals(-7L, model.getTenantId());
        assertEquals("operator-7", model.getCreateUserId());
        assertEquals(NOW, model.getCreateTime());
        assertEquals("operator-7", model.getUpdateUserId());
        assertEquals(NOW, model.getUpdateTime());
        assertFalse(model.getIsDeleted());
        assertEquals(1, tenant.reads());
        assertEquals(1, user.reads());
    }

    @Test
    void updateFillRefreshesOnlyTenantAndUpdateAuditFields() {
        TestTenantIdProvider tenant = new TestTenantIdProvider();
        tenant.set(0L);
        TestUserIdProvider user = new TestUserIdProvider();
        user.set("operator-0");
        EgonColaMetaObjectHandler handler = new EgonColaMetaObjectHandler(
                tenant, user, Clock.fixed(NOW, ZoneOffset.UTC));
        TestBusinessModel model = new TestBusinessModel().businessValues("title", "payload");
        Instant createdAt = NOW.minusSeconds(10);
        model.setId(99L);
        model.setTenantId(42L);
        model.setCreateUserId("creator");
        model.setCreateTime(createdAt);
        model.setUpdateUserId("old-operator");
        model.setUpdateTime(createdAt);
        model.setIsDeleted(true);

        handler.updateFill(SystemMetaObject.forObject(model));

        assertEquals(99L, model.getId());
        assertEquals(0L, model.getTenantId());
        assertEquals("creator", model.getCreateUserId());
        assertEquals(createdAt, model.getCreateTime());
        assertEquals("operator-0", model.getUpdateUserId());
        assertEquals(NOW, model.getUpdateTime());
        assertTrue(model.getIsDeleted());
        assertEquals(1, tenant.reads());
        assertEquals(1, user.reads());
    }

    @Test
    void fillHooksAreProtectedExtensionsAndNonEgonObjectsRemainUntouched() {
        TestTenantIdProvider tenant = new TestTenantIdProvider();
        tenant.set(1L);
        TestUserIdProvider user = new TestUserIdProvider();
        user.set("operator-1");
        RecordingHandler handler = new RecordingHandler(tenant, user, Clock.fixed(NOW, ZoneOffset.UTC));
        TestBusinessModel model = new TestBusinessModel().businessValues("title", "payload");

        handler.insertFill(SystemMetaObject.forObject(model));
        handler.updateFill(SystemMetaObject.forObject(model));
        assertEquals(1, handler.insertHooks);
        assertEquals(1, handler.updateHooks);

        Object ordinaryObject = new Object();
        assertDoesNotThrow(() -> handler.insertFill(SystemMetaObject.forObject(ordinaryObject)));
        assertDoesNotThrow(() -> handler.updateFill(SystemMetaObject.forObject(ordinaryObject)));
    }

    @Test
    void handlerRejectsMissingCustomContextBeforeWritingFields() {
        TestTenantIdProvider tenant = new TestTenantIdProvider();
        TestUserIdProvider user = new TestUserIdProvider();
        EgonColaMetaObjectHandler handler = new EgonColaMetaObjectHandler(
                tenant, user, Clock.fixed(NOW, ZoneOffset.UTC));

        assertContextFailure("TENANT_CONTEXT_MISSING", () -> {
            handler.insertFill(SystemMetaObject.forObject(
                    new TestBusinessModel().businessValues("title", "payload")));
            return null;
        });
    }

    private static void assertContextFailure(String code, ThrowingSupplier<?> action) {
        IllegalStateException exception = assertThrows(IllegalStateException.class, action::get);
        assertEquals(code, exception.getMessage());
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get();
    }

    private static final class RecordingHandler extends EgonColaMetaObjectHandler {

        private int insertHooks;
        private int updateHooks;

        private RecordingHandler(TestTenantIdProvider tenant, TestUserIdProvider user, Clock clock) {
            super(tenant, user, clock);
        }

        @Override
        protected void afterInsertFill(MetaObject metaObject) {
            insertHooks++;
        }

        @Override
        protected void afterUpdateFill(MetaObject metaObject) {
            updateHooks++;
        }
    }
}
