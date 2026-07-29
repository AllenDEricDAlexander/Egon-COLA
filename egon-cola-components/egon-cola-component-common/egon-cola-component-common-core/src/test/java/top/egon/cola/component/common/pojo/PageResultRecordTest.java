package top.egon.cola.component.common.pojo;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.pojo.PageResultRecord;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageResultRecordTest {

    @Test
    void pageResultRecordCalculatesComposedMetadata() {
        PageResultRecord<String> page = PageResultRecord.success(List.of("a", "b"), 21, 2, 10);

        assertEquals(List.of("a", "b"), page.records());
        assertEquals(21, page.page().total());
        assertEquals(2, page.page().pageNo());
        assertEquals(10, page.page().pageSize());
        assertEquals(3, page.page().pages());
        assertTrue(page.page().hasPrevious());
        assertTrue(page.page().hasNext());
    }

    @Test
    void emptyPageResultRecordUsesSafeDefaults() {
        PageResultRecord<String> page = PageResultRecord.success(null, -1, -1, 0);

        assertEquals(List.of(), page.records());
        assertEquals(0, page.page().total());
        assertEquals(1, page.page().pageNo());
        assertEquals(10, page.page().pageSize());
        assertEquals(0, page.page().pages());
        assertFalse(page.page().hasPrevious());
        assertFalse(page.page().hasNext());
    }

    @Test
    void recordsAreDefensivelyCopied() {
        PageResultRecord<String> page = PageResultRecord.success(List.of("a"), 1, 1, 10);

        assertThrows(UnsupportedOperationException.class, () -> page.records().add("b"));
    }
}
