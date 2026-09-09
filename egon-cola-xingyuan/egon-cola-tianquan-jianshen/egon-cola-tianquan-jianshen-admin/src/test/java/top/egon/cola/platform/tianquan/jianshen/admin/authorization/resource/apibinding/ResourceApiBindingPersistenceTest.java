package top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.apibinding;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.po.ResourcePO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.apibinding.domain.enums.ResourceApiBindingStatusEnum;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.apibinding.domain.po.ResourceApiBindingPO;

import java.lang.reflect.Field;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceApiBindingPersistenceTest {

    private static final Instant NOW = Instant.parse("2026-08-25T00:00:00Z");

    @Test
    void mapsGlobalApplicationSourceTargetAndRegistrationColumns() throws Exception {
        Table table = ResourceApiBindingPO.class.getAnnotation(Table.class);
        assertEquals("rbac3_resource_api_binding", table.name());
        assertColumn("applicationId", "application_id");
        assertColumn("sourceResourceId", "source_resource_id");
        assertColumn("apiResourceId", "api_resource_id");
        assertColumn("sourceBuildId", "source_build_id");
        assertColumn("sourceChecksum", "source_checksum");
        assertColumn("status", "status");
    }

    @Test
    void acceptsDistinctSourceAndApiAndKeepsMechanicalStatus() {
        ResourceApiBindingPO binding = new ResourceApiBindingPO(
                110L, 71L, 501L, 701L,
                "build-1", "sha256:one", "ci", NOW);

        assertEquals(ResourceApiBindingStatusEnum.ACTIVE, binding.getStatus());
        assertEquals(501L, binding.getSourceResourceId());
        assertEquals(701L, binding.getApiResourceId());
    }

    @Test
    void rejectsSelfBindingAndBlankRegistrationIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceApiBindingPO(
                110L, 71L, 501L, 501L,
                "build-1", "sha256:one", "ci", NOW));
        assertThrows(IllegalArgumentException.class, () -> new ResourceApiBindingPO(
                110L, 71L, 501L, 701L,
                " ", "sha256:one", "ci", NOW));
    }

    @Test
    void resourceKeepsActualPermissionSeparateFromCiSuggestion() throws Exception {
        Field suggestion = ResourcePO.class.getDeclaredField("suggestedPermissionCode");
        assertEquals("suggested_permission_code",
                suggestion.getAnnotation(Column.class).name());
        assertEquals(Long.class,
                ResourcePO.class.getDeclaredField("requiredPermissionId").getType());
    }

    private void assertColumn(String fieldName, String columnName)
            throws NoSuchFieldException {
        Field field = ResourceApiBindingPO.class.getDeclaredField(fieldName);
        assertEquals(columnName, field.getAnnotation(Column.class).name());
    }
}
