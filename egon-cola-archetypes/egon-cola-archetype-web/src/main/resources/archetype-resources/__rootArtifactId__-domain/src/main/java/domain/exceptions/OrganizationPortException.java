package ${package}.domain.exceptions;

public class OrganizationPortException extends RuntimeException {

    private final OrganizationDomainErrorCode code;

    public OrganizationPortException(OrganizationDomainErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public OrganizationDomainErrorCode code() { return code; }
}
