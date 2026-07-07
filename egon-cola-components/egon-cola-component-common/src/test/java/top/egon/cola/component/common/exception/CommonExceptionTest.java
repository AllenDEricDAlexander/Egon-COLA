package top.egon.cola.component.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CommonExceptionTest {

    @Test
    void businessExceptionKeepsCodeMessageAndCause() {
        IllegalArgumentException cause = new IllegalArgumentException("bad input");

        BusinessException exception = new BusinessException(404001, "订单不存在", cause);

        assertEquals(404001, exception.getCode());
        assertEquals("订单不存在", exception.getErrorMessage());
        assertEquals("订单不存在", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void businessExceptionUsesDefaultCode() {
        BusinessException exception = new BusinessException("业务处理失败");

        assertEquals(ErrorCodes.BUSINESS_ERROR.getCode(), exception.getCode());
        assertEquals("业务处理失败", exception.getErrorMessage());
        assertEquals("业务处理失败", exception.getMessage());
    }

    @Test
    void systemExceptionUsesDefaultCode() {
        SystemException exception = new SystemException("系统处理失败");

        assertEquals(ErrorCodes.SYSTEM_ERROR.getCode(), exception.getCode());
        assertEquals("系统处理失败", exception.getErrorMessage());
        assertEquals("系统处理失败", exception.getMessage());
    }

    @Test
    void errorCodesKeepCodeAndMessageTogether() {
        assertEquals(0, ErrorCodes.SUCCESS.getCode());
        assertEquals("处理成功", ErrorCodes.SUCCESS.getMessage());
        assertEquals(400, ErrorCodes.PARAM_ERROR.getCode());
        assertEquals("参数错误", ErrorCodes.PARAM_ERROR.getMessage());
    }
}
