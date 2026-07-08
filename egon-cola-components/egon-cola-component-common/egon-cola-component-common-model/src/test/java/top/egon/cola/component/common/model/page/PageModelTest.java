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

        assertEquals(List.of("a", "b"), page.records());
        assertEquals(21, page.total());
        assertEquals(2, page.pageNo());
        assertEquals(10, page.pageSize());
        assertEquals(3, page.pages());
        assertTrue(page.hasPrevious());
        assertTrue(page.hasNext());
    }

    @Test
    void emptyPageModelUsesSafeDefaults() {
        PageModel<String> page = PageModel.of(null, -1, -1, 0);

        assertEquals(List.of(), page.records());
        assertEquals(0, page.total());
        assertEquals(1, page.pageNo());
        assertEquals(10, page.pageSize());
        assertEquals(0, page.pages());
        assertFalse(page.hasPrevious());
        assertFalse(page.hasNext());
    }
}
