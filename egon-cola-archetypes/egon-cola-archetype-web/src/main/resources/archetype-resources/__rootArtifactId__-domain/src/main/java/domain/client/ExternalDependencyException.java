package ${package}.domain.client;

public final class ExternalDependencyException extends RuntimeException {

    private final String dependency;
    private final ExternalDependencyFailure failure;
    private final String externalCode;

    public ExternalDependencyException(
            String dependency,
            ExternalDependencyFailure failure,
            String externalCode,
            String message,
            Throwable cause) {
        super(message, cause);
        this.dependency = dependency;
        this.failure = failure;
        this.externalCode = externalCode;
    }

    public String dependency() { return dependency; }
    public ExternalDependencyFailure failure() { return failure; }
    public String externalCode() { return externalCode; }
}
