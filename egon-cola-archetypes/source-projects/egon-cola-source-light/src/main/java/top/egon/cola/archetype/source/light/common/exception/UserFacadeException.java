package top.egon.cola.archetype.source.light.common.exception;

import java.io.Serial;

/** User facade failure reported to an outbound caller. */
public class UserFacadeException extends BaseBusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public UserFacadeException(String code, String message) {
        super(code, message);
    }

    public UserFacadeException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
