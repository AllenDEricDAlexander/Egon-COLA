package top.egon.cola.archetype.source.web.application.exceptions;

public class OrganizationApplicationException extends RuntimeException {

    private final OrganizationFailureType failureType;
    private final String code;

    public OrganizationApplicationException(OrganizationFailureType failureType, String code, String message) {
        super(message);
        this.failureType = failureType;
        this.code = code;
    }

    public OrganizationFailureType failureType() { return failureType; }

    public String code() { return code; }
}
