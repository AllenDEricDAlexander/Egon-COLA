package top.egon.cola.component.common.enums;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.enums.EgonEnum;
import top.egon.cola.component.common.core.enums.ErrorStatus;
import top.egon.cola.component.common.core.enums.ResultCode;

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
        assertInstanceOf(EgonEnum.class, ResultCode.SUCCESS);
        assertInstanceOf(ErrorStatus.class, ResultCode.SUCCESS);
    }

    @Test
    void egonEnumExposesOnlyCodeAndMessageContracts() {
        Set<String> methods = Arrays.stream(EgonEnum.class.getDeclaredMethods())
                .map(method -> method.getName())
                .collect(Collectors.toSet());

        assertEquals(Set.of("getCode", "getMessage"), methods);
    }

    @Test
    void enumBuiltInMethodsRemainAvailableForSerializationAndLookup() {
        assertEquals(ResultCode.SUCCESS, ResultCode.valueOf(ResultCode.SUCCESS.name()));
        assertEquals(0, ResultCode.SUCCESS.ordinal());
        assertEquals(ResultCode.values().length, ResultCode.class.getEnumConstants().length);
        assertEquals(ResultCode.SUCCESS, ResultCode.values()[ResultCode.SUCCESS.ordinal()]);
    }

    @Test
    void resultCodeStoresOnlyCodeAndMessageFields() {
        Set<String> instanceFields = Arrays.stream(ResultCode.class.getDeclaredFields())
                .filter(field -> !field.isEnumConstant() && !field.isSynthetic())
                .map(field -> field.getName())
                .collect(Collectors.toSet());

        assertEquals(Set.of("code", "message"), instanceFields);
    }
}
