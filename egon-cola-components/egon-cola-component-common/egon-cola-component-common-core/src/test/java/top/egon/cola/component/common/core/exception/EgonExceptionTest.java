package top.egon.cola.component.common.core.exception;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.code.CommonStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class EgonExceptionTest {

    @Test
    void businessExceptionCarriesStatusCodeAndMessage() {
        EgonBusinessException exception = new EgonBusinessException(CommonStatus.BAD_REQUEST);

        assertEquals(CommonStatus.BAD_REQUEST.getCode(), exception.getCode());
        assertEquals(CommonStatus.BAD_REQUEST.getStatus(), exception.getStatus());
        assertEquals(CommonStatus.BAD_REQUEST.getMessage(), exception.getMessage());
    }

    @Test
    void systemExceptionCarriesCause() {
        RuntimeException cause = new RuntimeException("boom");

        EgonSystemException exception = new EgonSystemException(CommonStatus.SYSTEM_ERROR, cause);

        assertEquals(CommonStatus.SYSTEM_ERROR.getCode(), exception.getCode());
        assertEquals(CommonStatus.SYSTEM_ERROR.getStatus(), exception.getStatus());
        assertEquals(CommonStatus.SYSTEM_ERROR.getMessage(), exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void illegalStateExceptionSupportsCustomMessage() {
        EgonIllegalStateException exception = new EgonIllegalStateException(409001, "STATE_CONFLICT", "state conflict");

        assertEquals(409001, exception.getCode());
        assertEquals("STATE_CONFLICT", exception.getStatus());
        assertEquals("state conflict", exception.getMessage());
    }
}
