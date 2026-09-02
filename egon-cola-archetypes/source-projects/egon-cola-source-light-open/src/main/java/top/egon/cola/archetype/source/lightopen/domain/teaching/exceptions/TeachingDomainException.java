package top.egon.cola.archetype.source.lightopen.domain.teaching.exceptions;

import top.egon.cola.archetype.source.lightopen.common.exceptions.BaseBusinessException;

public class TeachingDomainException extends BaseBusinessException {
    public TeachingDomainException(String code, String message) {
        super(code, message);
    }

    public TeachingDomainException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
