package top.egon.cola.component.common.model.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageQueryTest {

    @Test
    void pageNoAndPageSizeAreNormalized() {
        PageQuery query = new PageQuery(0, 0);

        assertEquals(1, query.pageNo());
        assertEquals(10, query.pageSize());
        assertEquals(0, query.offset());
    }

    @Test
    void offsetUsesNormalizedValues() {
        PageQuery query = new PageQuery(3, 20);

        assertEquals(40, query.offset());
    }

    @Test
    void pageSizeIsCapped() {
        PageQuery query = new PageQuery(1, 999);

        assertEquals(PageQuery.MAX_PAGE_SIZE, query.pageSize());
    }
}
