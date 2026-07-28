package top.egon.cola.component.common.exception;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.enums.ResultCode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonExceptionTest {

    @Test
    void businessExceptionCarriesStatusCodeAndMessage() {
        BusinessException exception = new BusinessException(ResultCode.BAD_REQUEST);

        assertEquals(ResultCode.BAD_REQUEST.getCode(), exception.getCode());
        assertEquals(ResultCode.BAD_REQUEST.getStatus(), exception.getStatus());
        assertEquals(ResultCode.BAD_REQUEST.getMessage(), exception.getMessage());
        assertFalse(exception.isRetryable());
    }

    @Test
    void remoteCallExceptionCanBeRetryable() {
        RuntimeException cause = new RuntimeException("timeout");

        RemoteCallException exception = new RemoteCallException(ResultCode.REMOTE_CALL_ERROR, true, cause);

        assertEquals(ResultCode.REMOTE_CALL_ERROR.getCode(), exception.getCode());
        assertEquals(ResultCode.REMOTE_CALL_ERROR.getStatus(), exception.getStatus());
        assertEquals(ResultCode.REMOTE_CALL_ERROR.getMessage(), exception.getMessage());
        assertTrue(exception.isRetryable());
        assertSame(cause, exception.getCause());
    }

    @Test
    void typedExceptionsUseMatchingResultCode() {
        assertEquals(ResultCode.VALIDATION_ERROR.getCode(), new ValidationException(ResultCode.VALIDATION_ERROR).getCode());
        assertEquals(ResultCode.UNAUTHORIZED.getCode(), new UnauthorizedException(ResultCode.UNAUTHORIZED).getCode());
        assertEquals(ResultCode.FORBIDDEN.getCode(), new ForbiddenException(ResultCode.FORBIDDEN).getCode());
        assertEquals(ResultCode.NOT_FOUND.getCode(), new NotFoundException(ResultCode.NOT_FOUND).getCode());
        assertEquals(ResultCode.CONCURRENCY_ERROR.getCode(), new ConcurrencyException(ResultCode.CONCURRENCY_ERROR).getCode());
    }
}
