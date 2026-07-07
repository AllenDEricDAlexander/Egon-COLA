package top.egon.cola.component.common.model.page;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageModelTest {

    @Test
    void pageModelCalculatesMetadata() {
        PageModel<String> page = PageModel.of(List.of("a", "b"), 21, 2, 10);

        assertEquals(List.of("a", "b"), page.getRecords());
        assertEquals(21, page.getTotal());
        assertEquals(2, page.getPageNo());
        assertEquals(10, page.getPageSize());
        assertEquals(3, page.getPages());
        assertTrue(page.isHasPrevious());
        assertTrue(page.isHasNext());
    }

    @Test
    void emptyPageModelUsesSafeDefaults() {
        PageModel<String> page = PageModel.of(null, -1, -1, 0);

        assertEquals(List.of(), page.getRecords());
        assertEquals(0, page.getTotal());
        assertEquals(1, page.getPageNo());
        assertEquals(10, page.getPageSize());
        assertEquals(0, page.getPages());
        assertFalse(page.isHasPrevious());
        assertFalse(page.isHasNext());
    }
}
