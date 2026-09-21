package top.egon.cola.archetype.source.lightopen.common.exception;

import java.io.Serial;

/** Teaching application use-case failure. */
public class TeachingUseCaseException extends BaseBusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public TeachingUseCaseException(String code, String message) {
        super(code, message);
    }

    public TeachingUseCaseException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
