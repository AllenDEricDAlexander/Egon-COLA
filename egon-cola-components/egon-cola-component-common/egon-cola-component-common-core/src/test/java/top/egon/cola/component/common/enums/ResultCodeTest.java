package top.egon.cola.component.common.enums;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultCodeTest {

    @Test
    void codesAndStatusesAreUnique() {
        Set<Integer> codes = Arrays.stream(ResultCode.values()).map(ResultCode::getCode).collect(Collectors.toSet());
        Set<String> statuses = Arrays.stream(ResultCode.values()).map(ResultCode::getStatus).collect(Collectors.toSet());

        assertEquals(ResultCode.values().length, codes.size());
        assertEquals(ResultCode.values().length, statuses.size());
    }

    @Test
    void successCodeComesFromCommonResultContract() {
        assertEquals(10000, ResultCode.SUCCESS.getCode());
        assertEquals("SUCCESS", ResultCode.SUCCESS.getStatus());
        assertEquals("success", ResultCode.SUCCESS.getMessage());
        assertTrue(ResultCode.SUCCESS.isSuccess());
        assertInstanceOf(ErrorStatus.class, ResultCode.SUCCESS);
    }
}
