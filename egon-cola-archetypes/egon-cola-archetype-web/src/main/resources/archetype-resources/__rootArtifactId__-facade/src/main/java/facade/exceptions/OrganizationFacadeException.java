package ${package}.facade.exceptions;

public class OrganizationFacadeException extends RuntimeException {

    private final String code;
    private final String traceId;

    public OrganizationFacadeException(String code, String message, String traceId) {
        super(message);
        this.code = code;
        this.traceId = traceId;
    }

    public String code() { return code; }

    public String traceId() { return traceId; }
}
