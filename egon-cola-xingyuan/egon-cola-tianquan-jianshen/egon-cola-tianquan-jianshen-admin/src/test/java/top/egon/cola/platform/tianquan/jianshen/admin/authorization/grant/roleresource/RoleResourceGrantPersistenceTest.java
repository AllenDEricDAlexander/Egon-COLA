package top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.enums.RoleResourceGrantStatusEnum;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.po.RoleResourceGrantPO;

import java.lang.reflect.Field;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoleResourceGrantPersistenceTest {

    private static final Instant START = Instant.parse("2026-08-25T00:00:00Z");

    @Test
    void mapsTenantRoleResourceWindowAndLifecycleColumns() throws Exception {
        Table table = RoleResourceGrantPO.class.getAnnotation(Table.class);
        assertEquals("rbac3_role_resource_grant", table.name());
        assertColumn("tenantId", "tenant_id");
        assertColumn("applicationId", "application_id");
        assertColumn("roleId", "role_id");
        assertColumn("resourceId", "resource_id");
        assertColumn("validFrom", "valid_from");
        assertColumn("validTo", "valid_to");
        assertColumn("status", "status");
    }

    @Test
    void activatesAndDisablesDirectGrantWithoutPersistingDerivedFacts() {
        RoleResourceGrantPO grant = new RoleResourceGrantPO(
                100L, 200L, 71L, 301L, 501L,
                START, null, "admin", START);

        assertEquals(RoleResourceGrantStatusEnum.ACTIVE, grant.getStatus());
        assertEquals(501L, grant.getResourceId());
        Instant end = START.plusSeconds(3600);
        grant.disable("admin", end);
        assertEquals(RoleResourceGrantStatusEnum.DISABLED, grant.getStatus());
        assertEquals(end, grant.getValidTo());
    }

    @Test
    void rejectsInvalidIdentityAndWindow() {
        assertThrows(IllegalArgumentException.class, () -> new RoleResourceGrantPO(
                0L, 200L, 71L, 301L, 501L,
                START, null, "admin", START));
        assertThrows(IllegalArgumentException.class, () -> new RoleResourceGrantPO(
                100L, 200L, 71L, 301L, 501L,
                START, START, "admin", START));
    }

    private void assertColumn(String fieldName, String columnName)
            throws NoSuchFieldException {
        Class<?> type = RoleResourceGrantPO.class;
        Field field;
        while (true) {
            try {
                field = type.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException missing) {
                type = type.getSuperclass();
                if (type == null) {
                    throw missing;
                }
            }
        }
        assertEquals(columnName, field.getAnnotation(Column.class).name());
    }
}
