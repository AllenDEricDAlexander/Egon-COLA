package top.egon.cola.component.common.core.code;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonStatusTest {

    @Test
    void codesAndStatusesAreUnique() {
        Set<Integer> codes = Arrays.stream(CommonStatus.values()).map(CommonStatus::getCode).collect(Collectors.toSet());
        Set<String> statuses = Arrays.stream(CommonStatus.values()).map(CommonStatus::getStatus).collect(Collectors.toSet());

        assertEquals(CommonStatus.values().length, codes.size());
        assertEquals(CommonStatus.values().length, statuses.size());
    }

    @Test
    void successCodeIsZeroAndBusinessErrorUsesBusinessRange() {
        assertEquals(0, CommonStatus.SUCCESS.getCode());
        assertTrue(CommonStatus.SUCCESS.isSuccess());
        assertEquals(600000, CommonStatus.BUSINESS_ERROR.getCode());
    }
}
