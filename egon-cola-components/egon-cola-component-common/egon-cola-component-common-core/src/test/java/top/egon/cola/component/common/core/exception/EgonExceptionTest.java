package top.egon.cola.component.common.core.exception;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.code.CommonStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EgonExceptionTest {

    @Test
    void businessExceptionCarriesStatusCodeAndMessage() {
        EgonBusinessException exception = new EgonBusinessException(CommonStatus.BAD_REQUEST);

        assertEquals(CommonStatus.BAD_REQUEST.getCode(), exception.getCode());
        assertEquals(CommonStatus.BAD_REQUEST.getStatus(), exception.getStatus());
        assertEquals(CommonStatus.BAD_REQUEST.getMessage(), exception.getMessage());
        assertFalse(exception.isRetryable());
    }

    @Test
    void remoteCallExceptionCanBeRetryable() {
        RuntimeException cause = new RuntimeException("timeout");

        EgonRemoteCallException exception = new EgonRemoteCallException(CommonStatus.REMOTE_CALL_ERROR, true, cause);

        assertEquals(CommonStatus.REMOTE_CALL_ERROR.getCode(), exception.getCode());
        assertEquals(CommonStatus.REMOTE_CALL_ERROR.getStatus(), exception.getStatus());
        assertEquals(CommonStatus.REMOTE_CALL_ERROR.getMessage(), exception.getMessage());
        assertTrue(exception.isRetryable());
        assertSame(cause, exception.getCause());
    }

    @Test
    void typedExceptionsUseMatchingCommonStatus() {
        assertEquals(CommonStatus.VALIDATION_ERROR.getCode(), new EgonValidationException(CommonStatus.VALIDATION_ERROR).getCode());
        assertEquals(CommonStatus.UNAUTHORIZED.getCode(), new EgonUnauthorizedException(CommonStatus.UNAUTHORIZED).getCode());
        assertEquals(CommonStatus.FORBIDDEN.getCode(), new EgonForbiddenException(CommonStatus.FORBIDDEN).getCode());
        assertEquals(CommonStatus.NOT_FOUND.getCode(), new EgonNotFoundException(CommonStatus.NOT_FOUND).getCode());
        assertEquals(CommonStatus.CONCURRENCY_ERROR.getCode(), new EgonConcurrencyException(CommonStatus.CONCURRENCY_ERROR).getCode());
    }
}
