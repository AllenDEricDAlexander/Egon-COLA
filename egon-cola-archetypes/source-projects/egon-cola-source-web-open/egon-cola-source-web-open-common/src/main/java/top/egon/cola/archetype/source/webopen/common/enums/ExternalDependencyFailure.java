package top.egon.cola.archetype.source.webopen.common.enums;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Classification of a failure observed while calling an external dependency. */
public enum ExternalDependencyFailure implements EgonEnum {

    NOT_FOUND(0, "external resource not found"),
    BUSINESS_REJECTED(1, "external service rejected the request"),
    VALIDATION_FAILED(2, "external contract validation failed"),
    UNAVAILABLE(3, "external service is unavailable"),
    TIMEOUT(4, "external service call timed out"),
    CONTRACT_INCOMPATIBLE(5, "external contract is incompatible"),
    SERVICE_FAILURE(6, "external service failed");

    private final int code;

    private final String message;

    ExternalDependencyFailure(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
