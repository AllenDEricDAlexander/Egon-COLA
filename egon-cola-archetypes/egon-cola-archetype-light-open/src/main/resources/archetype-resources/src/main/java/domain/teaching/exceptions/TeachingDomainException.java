package ${package}.domain.teaching.exceptions;

import ${package}.common.exceptions.BaseBusinessException;

public class TeachingDomainException extends BaseBusinessException {
    public TeachingDomainException(String code, String message) {
        super(code, message);
    }

    public TeachingDomainException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
