package ${package}.domain.client;

public enum ExternalDependencyFailure {
    NOT_FOUND,
    BUSINESS_REJECTED,
    VALIDATION_FAILED,
    UNAVAILABLE,
    TIMEOUT,
    CONTRACT_INCOMPATIBLE,
    SERVICE_FAILURE
}
