package top.egon.cola.archetype.source.webopen.common.enums;

import top.egon.cola.component.common.core.enums.ErrorStatus;

/** Classification of an application use-case rejection. */
public enum OrganizationFailureType implements ErrorStatus {

    VALIDATION(0, "request validation failed"),
    FORBIDDEN(1, "actor is not permitted"),
    NOT_FOUND(2, "resource not found"),
    CONFLICT(3, "request conflicts with existing state"),
    DOMAIN_REJECTED(4, "domain rule rejected the request"),
    DEPENDENCY_UNAVAILABLE(5, "dependency is unavailable"),
    INTERNAL(6, "internal failure");

    private final int code;

    private final String message;

    OrganizationFailureType(int code, String message) {
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

    @Override
    public String getStatus() {
        return name();
    }
}
