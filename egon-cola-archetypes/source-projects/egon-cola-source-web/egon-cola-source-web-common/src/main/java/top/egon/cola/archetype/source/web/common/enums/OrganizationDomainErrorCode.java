package top.egon.cola.archetype.source.web.common.enums;

import top.egon.cola.component.common.core.enums.ErrorStatus;

/** Stable wire code for an organization domain invariant violation. */
public enum OrganizationDomainErrorCode implements ErrorStatus {

    INVALID_USER_ID(0, "user identifier is invalid"),
    INVALID_USER_NAME(1, "user name is invalid"),
    INVALID_EMAIL(2, "email is invalid"),
    INVALID_CODE(3, "code is invalid"),
    DUPLICATE_ROLE_ASSIGNMENT(4, "role is already assigned"),
    DUPLICATE_PERMISSION_GRANT(5, "permission is already granted"),
    USER_DISABLED(6, "user is disabled"),
    ROLE_ARCHIVED(7, "role is archived"),
    PERMISSION_INACTIVE(8, "permission is inactive"),
    DOMAIN_REJECTED(9, "domain rule rejected the request"),
    CONFLICT(10, "request conflicts with existing state"),
    DEPENDENCY_UNAVAILABLE(11, "dependency is unavailable");

    private final int code;

    private final String message;

    OrganizationDomainErrorCode(int code, String message) {
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
