package top.egon.cola.component.common.model;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.model.page.PageMeta;
import top.egon.cola.component.common.model.page.PageModel;
import top.egon.cola.component.common.model.page.PageSlice;
import top.egon.cola.component.common.model.query.PageQuery;
import top.egon.cola.component.common.model.query.SortQuery;
import top.egon.cola.component.common.model.query.TimeRangeQuery;
import top.egon.cola.component.common.model.request.BaseRequest;
import top.egon.cola.component.common.model.request.OperatorContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelSerializationTest {

    @Test
    void pageModelsCanRoundTripThroughJavaSerialization() throws Exception {
        assertEquals(1, roundTrip(PageMeta.of(1, 1, 10)).total());
        assertEquals(List.of("a"), roundTrip(PageModel.of(List.of("a"), 1, 1, 10)).records());
        assertEquals(List.of("a"), roundTrip(PageSlice.of(List.of("a"), true)).records());
    }

    @Test
    void queryModelsCanRoundTripThroughJavaSerialization() throws Exception {
        assertEquals(20, roundTrip(new PageQuery(2, 20)).pageSize());
        assertEquals("DESC", roundTrip(new SortQuery("name", "desc")).sortDirection());
        assertEquals(LocalDateTime.of(2026, 7, 8, 10, 0), roundTrip(new TimeRangeQuery(LocalDateTime.of(2026, 7, 8, 10, 0), null)).startTime());
    }

    @Test
    void requestModelsCanRoundTripThroughJavaSerialization() throws Exception {
        BaseRequest copy = roundTrip(new BaseRequest(new OperatorContext("u1", "Mario", "t1")));

        assertEquals("u1", copy.operator().operatorId());
    }

    @SuppressWarnings("unchecked")
    private static <T extends Serializable> T roundTrip(T value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(value);
        }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) input.readObject();
        }
    }
}
