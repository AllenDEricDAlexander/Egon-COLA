package top.egon.cola.archetype.source.lightopen.common.exception;

import java.io.Serial;

/** User application use-case failure. */
public class UserUseCaseException extends BaseBusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public UserUseCaseException(String code, String message) {
        super(code, message);
    }

    public UserUseCaseException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
