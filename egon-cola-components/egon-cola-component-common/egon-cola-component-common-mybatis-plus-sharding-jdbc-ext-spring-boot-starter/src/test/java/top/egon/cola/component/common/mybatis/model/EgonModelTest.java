package top.egon.cola.component.common.mybatis.model;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaMdcUserIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaUserIdProvider;
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

    @Test
    void builderBuildsBusinessFieldsAndKeepsTechnicalFieldsFrameworkOwned() {
        TestBusinessModel model = TestBusinessModel.builder()
                .title("title")
                .payload("payload")
                .build();

        assertEquals("title", model.getTitle());
        assertEquals("payload", model.getPayload());
        assertEquals(null, model.getVersion());
        model.setVersion(3L);

        model.setId(101L);
        model.setTenantId(9L);
        model.setCreateUserId("creator");
        model.setCreateTime(NOW);
        model.setUpdateUserId("updater");
        model.setUpdateTime(NOW);
        model.setDeletedAt(null);
        assertEquals(101L, model.getId());
        assertEquals(9L, model.getTenantId());
        assertEquals("creator", model.getCreateUserId());
        assertEquals(NOW, model.getCreateTime());
        assertEquals("updater", model.getUpdateUserId());
        assertEquals(NOW, model.getUpdateTime());
        assertEquals(null, model.getDeletedAt());

        Object builder = TestBusinessModel.builder();
        assertThrows(NoSuchMethodException.class,
                () -> builder.getClass().getMethod("id", Long.class));
        assertThrows(NoSuchMethodException.class,
                () -> builder.getClass().getMethod("tenantId", Long.class));
    }

    @AfterEach
    void clearContext() {
        MDC.clear();
    }

    @Test
    void propertiesHaveSafeTechnicalDefaults() {
        EgonColaMybatisPlusProperties properties = new EgonColaMybatisPlusProperties();

        assertTrue(properties.isEnabled());
        assertEquals(EgonColaTenantIdProvider.DEFAULT_MDC_KEY, properties.getTenantId().getMdcKey());
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
    }

    @Test
    void staticTenantEntryAcceptsAnyLongAndFailsClosed() {
        TestTenantIdProvider tenant = new TestTenantIdProvider();

        tenant.set(0L);
        assertEquals(0L, EgonColaTenantIdProvider.currentTenantId());
        tenant.set(-7L);
        assertEquals(-7L, EgonColaTenantIdProvider.currentTenantId());
        tenant.set(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, EgonColaTenantIdProvider.currentTenantId());
        tenant.clear();
        assertContextFailure("TENANT_CONTEXT_MISSING", EgonColaTenantIdProvider::currentTenantId);
        MDC.put(EgonColaTenantIdProvider.DEFAULT_MDC_KEY, "not-a-long");
        assertContextFailure("TENANT_CONTEXT_MALFORMED", EgonColaTenantIdProvider::currentTenantId);
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
                user, Clock.fixed(NOW, ZoneOffset.UTC));
        TestBusinessModel model = new TestBusinessModel().businessValues("title", "payload");
        model.setId(99L);
        model.setTenantId(42L);
        model.setCreateUserId("forged-create");
        model.setCreateTime(NOW.minusSeconds(1));
        model.setUpdateUserId("forged-update");
        model.setUpdateTime(NOW.minusSeconds(1));
        model.setDeletedAt(java.time.LocalDateTime.of(2026, 1, 1, 0, 0));

        handler.insertFill(SystemMetaObject.forObject(model));

        assertEquals(99L, model.getId());
        assertEquals(-7L, model.getTenantId());
        assertEquals("operator-7", model.getCreateUserId());
        assertEquals(NOW, model.getCreateTime());
        assertEquals("operator-7", model.getUpdateUserId());
        assertEquals(NOW, model.getUpdateTime());
        assertEquals(null, model.getDeletedAt());
        assertEquals(1, user.reads());
    }

    @Test
    void updateFillPreservesIdentityAndVersionWhileRefreshingUpdateAudit() {
        TestTenantIdProvider tenant = new TestTenantIdProvider();
        tenant.set(0L);
        TestUserIdProvider user = new TestUserIdProvider();
        user.set("operator-0");
        EgonColaMetaObjectHandler handler = new EgonColaMetaObjectHandler(
                user, Clock.fixed(NOW, ZoneOffset.UTC));
        TestBusinessModel model = new TestBusinessModel().businessValues("title", "payload");
        Instant createdAt = NOW.minusSeconds(10);
        model.setId(99L);
        model.setTenantId(0L);
        model.setVersion(3L);
        model.setCreateUserId("creator");
        model.setCreateTime(createdAt);
        model.setUpdateUserId("old-operator");
        model.setUpdateTime(createdAt);
        model.setDeletedAt(java.time.LocalDateTime.of(2026, 1, 1, 0, 0));

        handler.updateFill(SystemMetaObject.forObject(model));

        assertEquals(99L, model.getId());
        assertEquals(0L, model.getTenantId());
        assertEquals(3L, model.getVersion());
        assertEquals("creator", model.getCreateUserId());
        assertEquals(createdAt, model.getCreateTime());
        assertEquals("operator-0", model.getUpdateUserId());
        assertEquals(NOW, model.getUpdateTime());
        assertEquals(java.time.LocalDateTime.of(2026, 1, 1, 0, 0), model.getDeletedAt());
        assertEquals(1, user.reads());
    }

    @Test
    void fillResolvesTheTenantContextFromTheStaticEntryOnEveryCall() {
        TestTenantIdProvider tenant = new TestTenantIdProvider();
        tenant.set(4L);
        TestUserIdProvider user = new TestUserIdProvider();
        user.set("operator-4");
        TenantErasingHandler handler = new TenantErasingHandler(user, Clock.fixed(NOW, ZoneOffset.UTC));

        handler.insertFill(SystemMetaObject.forObject(
                new TestBusinessModel().businessValues("title", "payload")));

        assertEquals(1, handler.hooks);
        assertEquals(4L, handler.tenantValueSeenInHook);
        assertContextFailure("TENANT_CONTEXT_MISSING", () -> {
            handler.insertFill(SystemMetaObject.forObject(
                    new TestBusinessModel().businessValues("title", "payload")));
            return null;
        });

        tenant.set(4L);
        TestBusinessModel updated = new TestBusinessModel().businessValues("title", "payload");
        updated.setTenantId(4L);
        handler.updateFill(SystemMetaObject.forObject(updated));
        assertEquals(2, handler.hooks);
    }

    @Test
    void fillHooksAreProtectedExtensionsAndNonEgonObjectsRemainUntouched() {
        TestTenantIdProvider tenant = new TestTenantIdProvider();
        tenant.set(1L);
        TestUserIdProvider user = new TestUserIdProvider();
        user.set("operator-1");
        RecordingHandler handler = new RecordingHandler(user, Clock.fixed(NOW, ZoneOffset.UTC));
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
        TestUserIdProvider user = new TestUserIdProvider();
        user.set("operator");
        EgonColaMetaObjectHandler handler = new EgonColaMetaObjectHandler(
                user, Clock.fixed(NOW, ZoneOffset.UTC));

        assertContextFailure("TENANT_CONTEXT_MISSING", () -> {
            handler.insertFill(SystemMetaObject.forObject(
                    new TestBusinessModel().businessValues("title", "payload")));
            return null;
        });
    }

    @Test
    void optionalFillHooksCannotOverrideCommonFields() {
        TestTenantIdProvider tenant = new TestTenantIdProvider();
        tenant.set(1L);
        TestUserIdProvider user = new TestUserIdProvider();
        user.set("user");
        EgonColaMetaObjectHandler handler = new EgonColaMetaObjectHandler(user, Clock.systemUTC()) {
            @Override
            protected void afterInsertFill(MetaObject object) { object.setValue("tenantId", 99L); }
        };
        assertContextFailure("TECHNICAL_FIELD_OVERRIDE_FORBIDDEN", () -> {
            handler.insertFill(SystemMetaObject.forObject(new TestBusinessModel().businessValues("title", null)));
            return null;
        });
    }

    @Test
    void updatesRejectTenantChangesInsteadOfReplacingTheirIdentity() {
        TestTenantIdProvider tenant = new TestTenantIdProvider();
        tenant.set(1L);
        TestUserIdProvider user = new TestUserIdProvider();
        user.set("user");
        EgonColaMetaObjectHandler handler = new EgonColaMetaObjectHandler(user, Clock.systemUTC());
        TestBusinessModel model = new TestBusinessModel().businessValues("title", null);
        model.setTenantId(99L);
        assertContextFailure("TENANT_CONTEXT_MISMATCH", () -> {
            handler.updateFill(SystemMetaObject.forObject(model));
            return null;
        });
        assertEquals(99L, model.getTenantId());
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

        private RecordingHandler(TestUserIdProvider user, Clock clock) {
            super(user, clock);
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

    private static final class TenantErasingHandler extends EgonColaMetaObjectHandler {

        private int hooks;
        private Long tenantValueSeenInHook;

        private TenantErasingHandler(TestUserIdProvider user, Clock clock) {
            super(user, clock);
        }

        @Override
        protected void afterInsertFill(MetaObject metaObject) {
            record(metaObject);
        }

        @Override
        protected void afterUpdateFill(MetaObject metaObject) {
            record(metaObject);
        }

        private void record(MetaObject metaObject) {
            hooks++;
            tenantValueSeenInHook = (Long) metaObject.getValue("tenantId");
            MDC.remove(EgonColaTenantIdProvider.DEFAULT_MDC_KEY);
        }
    }
}
