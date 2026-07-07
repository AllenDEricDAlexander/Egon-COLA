package top.egon.cola.component.common.model.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageQueryTest {

    @Test
    void pageNoAndPageSizeAreNormalized() {
        PageQuery query = new PageQuery();
        query.setPageNo(0);
        query.setPageSize(0);

        assertEquals(1, query.getPageNo());
        assertEquals(10, query.getPageSize());
        assertEquals(0, query.offset());
    }

    @Test
    void offsetUsesNormalizedValues() {
        PageQuery query = new PageQuery();
        query.setPageNo(3);
        query.setPageSize(20);

        assertEquals(40, query.offset());
    }
}
