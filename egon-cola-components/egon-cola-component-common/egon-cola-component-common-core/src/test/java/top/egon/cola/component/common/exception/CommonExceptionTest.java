package top.egon.cola.component.common.exception;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.enums.BusinessExceptionEnum;
import top.egon.cola.component.common.core.enums.ExceptionLevelEnum;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.BusinessException;
import top.egon.cola.component.common.core.exception.CommonException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonExceptionTest {

    @Test
    void businessExceptionCarriesStatusCodeAndMessage() {
        BusinessException exception = new BusinessException(BusinessExceptionEnum.INVALID_PARAM);

        assertEquals(BusinessExceptionEnum.INVALID_PARAM.getCode(), exception.getCode());
        assertEquals(BusinessExceptionEnum.INVALID_PARAM.getStatus(), exception.getStatus());
        assertEquals(BusinessExceptionEnum.INVALID_PARAM.getMessage(), exception.getMessage());
        assertEquals(ExceptionLevelEnum.ERROR, exception.getLevel());
        assertFalse(exception.isRetryable());
    }

    @Test
    void commonExceptionCanBeRetryable() {
        RuntimeException cause = new RuntimeException("timeout");

        CommonException exception = new CommonException(ResultCode.REMOTE_CALL_ERROR, true, cause);

        assertEquals(ResultCode.REMOTE_CALL_ERROR.getCode(), exception.getCode());
        assertEquals(ResultCode.REMOTE_CALL_ERROR.getStatus(), exception.getStatus());
        assertEquals(ResultCode.REMOTE_CALL_ERROR.getMessage(), exception.getMessage());
        assertTrue(exception.isRetryable());
        assertSame(cause, exception.getCause());
    }

    @Test
    void businessExceptionFormatsEnumMessageDetails() {
        BusinessException exception = new BusinessException(
                BusinessExceptionEnum.INVALID_PARAM,
                "orderId"
        );

        assertEquals("参数orderId为空或者不合法", exception.getMessage());
    }

    @Test
    void commonExceptionFormatsEnumMessageDetailsWithCause() {
        RuntimeException cause = new RuntimeException("connection refused");

        CommonException exception = new CommonException(
                BusinessExceptionEnum.INTERFACE_CALL_ERROR,
                cause,
                "inventory-service"
        );

        assertEquals("接口调用异常,inventory-service", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void formattingWithoutDetailsKeepsTemplateForLaterContext() {
        CommonException exception = new CommonException(BusinessExceptionEnum.SYSTEM_ERROR);

        assertEquals("系统处理异常:%s", exception.getMessage());
    }

    @Test
    void businessExceptionPreservesLevelDetailsAndCause() {
        RuntimeException cause = new RuntimeException("invalid order");
        Object[] details = {"orderId"};

        BusinessException exception = new BusinessException(
                BusinessExceptionEnum.USER_DEFINED_MESSAGE,
                ExceptionLevelEnum.WARN,
                cause,
                details
        );

        assertEquals(ExceptionLevelEnum.WARN, exception.getLevel());
        assertArrayEquals(details, exception.getDetails());
        assertSame(cause, exception.getCause());
        assertEquals(BusinessExceptionEnum.USER_DEFINED_MESSAGE, exception.getBusinessExceptionEnum());
    }

    @Test
    void businessExceptionEnumSupportsNumericLookup() {
        assertEquals(BusinessExceptionEnum.INVALID_PARAM,
                BusinessExceptionEnum.fromCode(BusinessExceptionEnum.INVALID_PARAM.getCode()));
        assertEquals(BusinessExceptionEnum.INVALID_PARAM,
                BusinessExceptionEnum.fromValue(BusinessExceptionEnum.INVALID_PARAM.getCode()));
    }
}
