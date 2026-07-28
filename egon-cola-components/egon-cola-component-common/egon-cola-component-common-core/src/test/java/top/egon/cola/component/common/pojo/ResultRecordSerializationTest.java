package top.egon.cola.component.common.pojo;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultRecordSerializationTest {

    @Test
    void resultRecordCanRoundTripThroughJavaSerialization() throws Exception {
        ResultRecord<String> copy = roundTrip(ResultRecord.success("ok"));

        assertEquals("ok", copy.data());
    }

    @Test
    void pageResultRecordCanRoundTripThroughJavaSerialization() throws Exception {
        PageResultRecord<String> copy = roundTrip(PageResultRecord.success(List.of("a"), 1, 1, 10));

        assertEquals(List.of("a"), copy.records());
        assertEquals(1, copy.page().total());
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
