package top.egon.cola.component.common.enums;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.enums.ExceptionLevelEnum;
import top.egon.cola.component.common.core.enums.EgonEnum;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExceptionLevelEnumTest {

    @Test
    void containsAllSupportedExceptionLevels() {
        assertEquals(
                Arrays.asList(
                        ExceptionLevelEnum.TRACE,
                        ExceptionLevelEnum.DEBUG,
                        ExceptionLevelEnum.INFO,
                        ExceptionLevelEnum.WARN,
                        ExceptionLevelEnum.ERROR,
                        ExceptionLevelEnum.FATAL
                ),
                Arrays.asList(ExceptionLevelEnum.values())
        );
    }

    @Test
    void exposesEgonEnumCodeAndMessageContract() {
        Set<Integer> codes = Arrays.stream(ExceptionLevelEnum.values())
                .map(ExceptionLevelEnum::getCode)
                .collect(Collectors.toSet());

        assertEquals(ExceptionLevelEnum.values().length, codes.size());
        assertEquals("trace", ExceptionLevelEnum.TRACE.getMessage());
        assertEquals("fatal", ExceptionLevelEnum.FATAL.getMessage());
        assertEquals(ExceptionLevelEnum.TRACE, ExceptionLevelEnum.fromValue(5));
        assertEquals(ExceptionLevelEnum.FATAL, ExceptionLevelEnum.fromValue(6));
        assertNull(ExceptionLevelEnum.fromValue(99));
        assertEquals(ExceptionLevelEnum.TRACE, EgonEnum.valueOf(ExceptionLevelEnum.class, "TRACE"));
    }

    @Test
    void storesOnlyCodeAndMessageFields() {
        Set<String> instanceFields = Arrays.stream(ExceptionLevelEnum.class.getDeclaredFields())
                .filter(field -> !field.isEnumConstant() && !field.isSynthetic())
                .map(field -> field.getName())
                .collect(Collectors.toSet());

        assertEquals(Set.of("code", "message"), instanceFields);
    }
}
