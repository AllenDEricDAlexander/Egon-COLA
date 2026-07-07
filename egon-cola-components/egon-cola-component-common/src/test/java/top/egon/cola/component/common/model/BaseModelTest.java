package top.egon.cola.component.common.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BaseModelTest {

    @Test
    void baseModelStoresExtensionFields() {
        BaseModel model = new BaseModel();

        model.putExtension("source", "api");

        assertEquals("api", model.getExtension("source"));
        assertEquals(Map.of("source", "api"), model.getExtensions());
    }

    @Test
    void baseModelIgnoresBlankExtensionKey() {
        BaseModel model = new BaseModel();

        model.putExtension(" ", "api");

        assertNull(model.getExtension(" "));
    }

    @Test
    void auditableModelKeepsAuditFields() {
        AuditableModel model = new AuditableModel();
        LocalDateTime now = LocalDateTime.of(2026, 7, 7, 12, 0);

        model.setCreatedAt(now);
        model.setCreatedBy("egon");
        model.setUpdatedAt(now.plusHours(1));
        model.setUpdatedBy("mario");
        model.setDeleted(false);

        assertEquals(now, model.getCreatedAt());
        assertEquals("egon", model.getCreatedBy());
        assertEquals(now.plusHours(1), model.getUpdatedAt());
        assertEquals("mario", model.getUpdatedBy());
        assertEquals(false, model.getDeleted());
    }

    @Test
    void baseEntityKeepsId() {
        BaseEntity<Long> entity = new BaseEntity<>();

        entity.setId(1L);

        assertEquals(1L, entity.getId());
    }
}
