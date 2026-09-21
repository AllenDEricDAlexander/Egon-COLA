package top.egon.cola.archetype.source.light.common.exception;

import java.io.Serial;

/** Teaching aggregate and domain-service failure. */
public class TeachingDomainException extends BaseBusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public TeachingDomainException(String code, String message) {
        super(code, message);
    }

    public TeachingDomainException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
