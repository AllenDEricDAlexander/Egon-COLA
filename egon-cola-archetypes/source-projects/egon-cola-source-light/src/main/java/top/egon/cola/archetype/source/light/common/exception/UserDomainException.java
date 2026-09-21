package top.egon.cola.archetype.source.light.common.exception;

import java.io.Serial;

/** User aggregate and domain-service failure. */
public class UserDomainException extends BaseBusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public UserDomainException(String code, String message) {
        super(code, message);
    }

    public UserDomainException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
