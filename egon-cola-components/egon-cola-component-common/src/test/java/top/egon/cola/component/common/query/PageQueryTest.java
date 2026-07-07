package top.egon.cola.component.common.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageQueryTest {

    @Test
    void normalizesPageValuesAndCalculatesOffset() {
        PageQuery query = new PageQuery();
        query.setPageNo(0);
        query.setPageSize(0);

        assertEquals(1, query.getPageNo());
        assertEquals(1, query.getPageSize());
        assertEquals(0, query.getOffset());
    }

    @Test
    void acceptsOnlyKnownOrderDirections() {
        PageQuery query = new PageQuery();

        query.setOrderDirection("ASC");
        assertEquals(PageQuery.ASC, query.getOrderDirection());

        query.setOrderDirection("bad");
        assertEquals(PageQuery.ASC, query.getOrderDirection());
    }
}
