package top.egon.cola.archetype.source.web.application.exceptions;

public enum OrganizationFailureType {
    VALIDATION,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    DOMAIN_REJECTED,
    DEPENDENCY_UNAVAILABLE,
    INTERNAL
}
