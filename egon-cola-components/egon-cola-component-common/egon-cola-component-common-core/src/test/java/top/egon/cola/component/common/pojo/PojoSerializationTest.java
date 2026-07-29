package top.egon.cola.component.common.pojo;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.pojo.BaseRequest;
import top.egon.cola.component.common.core.pojo.OperatorContext;
import top.egon.cola.component.common.core.pojo.PageMetaRecord;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.pojo.PageSlice;
import top.egon.cola.component.common.core.pojo.SortQuery;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PojoSerializationTest {

    @Test
    void pageRecordsCanRoundTripThroughJavaSerialization() throws Exception {
        assertEquals(1, roundTrip(PageMetaRecord.of(1, 1, 10)).total());
        assertEquals(List.of("a"), roundTrip(PageResultRecord.success(List.of("a"), 1, 1, 10)).records());
        assertEquals(List.of("a"), roundTrip(PageSlice.of(List.of("a"), true)).records());
    }

    @Test
    void queryRecordsCanRoundTripThroughJavaSerialization() throws Exception {
        assertEquals(20, roundTrip(new PageQuery(2, 20)).pageSize());
        assertEquals("DESC", roundTrip(new SortQuery("name", "desc")).sortDirection());
    }

    @Test
    void requestRecordsCanRoundTripThroughJavaSerialization() throws Exception {
        BaseRequest copy = roundTrip(new BaseRequest(new OperatorContext("u1", "Mario", "t1")));

        assertEquals("u1", copy.operator().userId());
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
