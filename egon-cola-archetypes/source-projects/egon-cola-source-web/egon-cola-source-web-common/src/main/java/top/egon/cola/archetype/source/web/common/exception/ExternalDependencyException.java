package top.egon.cola.archetype.source.web.common.exception;

import top.egon.cola.archetype.source.web.common.enums.ExternalDependencyFailure;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.BusinessException;

import java.io.Serial;

/**
 * Failure observed while calling an external dependency. The stable String wire code stays
 * published through {@link #getStatus()}, which carries the dependency and its classification.
 */
public final class ExternalDependencyException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String dependency;

    private final ExternalDependencyFailure failure;

    private final String externalCode;

    public ExternalDependencyException(
            String dependency,
            ExternalDependencyFailure failure,
            String externalCode,
            String message,
            Throwable cause) {
        super(ResultCode.BUSINESS_ERROR.getCode(), dependency + "_" + failure.name(), message, cause);
        this.dependency = dependency;
        this.failure = failure;
        this.externalCode = externalCode;
    }

    public String dependency() {
        return dependency;
    }

    public ExternalDependencyFailure failure() {
        return failure;
    }

    public String externalCode() {
        return externalCode;
    }
}
