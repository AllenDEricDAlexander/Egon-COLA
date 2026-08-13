package top.egon.cola.platform.rbac3.admin.architecture;

import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.shared.domain.po.TenantScopedPO;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 保护重命名持久化对象后的 JPQL 实体名及共享租户基类元数据。
 * Protects JPQL entity names and shared tenant base metadata after persistence-object renames.
 */
class JpaEntityNameCompatibilityTest {

    private static final Map<Class<?>, String> RENAMED_ENTITIES = Map.of();

    @Test
    void renamedEntitiesRetainTheirLegacyJpaNames() {
        RENAMED_ENTITIES.forEach((type, legacyName) -> {
            Entity entity = type.getAnnotation(Entity.class);
            assertThat(entity)
                    .as("@Entity on %s", type.getName())
                    .isNotNull();
            assertThat(entity.name())
                    .as("legacy JPQL entity name on %s", type.getName())
                    .isEqualTo(legacyName);
        });
    }

    @Test
    void tenantScopedPoRemainsAMappedSuperclass() {
        assertThat(TenantScopedPO.class)
                .hasAnnotation(MappedSuperclass.class);
    }
}
