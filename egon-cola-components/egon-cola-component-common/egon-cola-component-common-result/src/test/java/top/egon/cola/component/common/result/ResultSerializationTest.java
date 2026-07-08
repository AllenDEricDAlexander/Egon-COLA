package top.egon.cola.component.common.result;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.result.dto.PageResultDto;
import top.egon.cola.component.common.result.dto.ResultDto;
import top.egon.cola.component.common.result.factory.ResultDtos;
import top.egon.cola.component.common.result.factory.ResultModels;
import top.egon.cola.component.common.result.model.PageResultModel;
import top.egon.cola.component.common.result.model.ResultModel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultSerializationTest {

    @Test
    void resultDtoCanRoundTripThroughJavaSerialization() throws Exception {
        ResultDto<String> copy = roundTrip(ResultDtos.success("ok"));

        assertEquals("ok", copy.data());
    }

    @Test
    void pageResultDtoCanRoundTripThroughJavaSerialization() throws Exception {
        PageResultDto<String> copy = roundTrip(ResultDtos.page(List.of("a"), 1, 1, 10));

        assertEquals(List.of("a"), copy.records());
    }

    @Test
    void resultModelCanRoundTripThroughJavaSerialization() throws Exception {
        ResultModel<String> copy = roundTrip(ResultModels.success("ok"));

        assertEquals("ok", copy.data());
    }

    @Test
    void pageResultModelCanRoundTripThroughJavaSerialization() throws Exception {
        PageResultModel<String> copy = roundTrip(ResultModels.page(List.of("a"), 1, 1, 10));

        assertEquals(List.of("a"), copy.records());
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
