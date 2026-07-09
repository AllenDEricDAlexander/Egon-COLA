package ${package}.domain.user.exceptions;

import ${package}.common.exceptions.BaseBusinessException;

public class UserDomainException extends BaseBusinessException {
    public UserDomainException(String code, String message) {
        super(code, message);
    }

    public UserDomainException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
