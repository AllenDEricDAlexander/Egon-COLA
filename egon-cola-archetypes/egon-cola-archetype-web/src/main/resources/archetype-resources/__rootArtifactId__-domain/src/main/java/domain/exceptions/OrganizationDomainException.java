package ${package}.domain.exceptions;

public class OrganizationDomainException extends RuntimeException {

    private final OrganizationDomainErrorCode code;

    public OrganizationDomainException(OrganizationDomainErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public OrganizationDomainErrorCode code() { return code; }
}
