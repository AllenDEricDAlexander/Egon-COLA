package top.egon.cola.archetype.source.light.domain.user.exceptions;

import top.egon.cola.archetype.source.light.common.exceptions.BaseBusinessException;

public class UserDomainException extends BaseBusinessException {
    public UserDomainException(String code, String message) {
        super(code, message);
    }

    public UserDomainException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
