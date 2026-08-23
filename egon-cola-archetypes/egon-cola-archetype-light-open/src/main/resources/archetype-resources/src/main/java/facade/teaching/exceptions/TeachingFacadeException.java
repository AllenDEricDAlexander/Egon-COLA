package ${package}.facade.teaching.exceptions;

public class TeachingFacadeException extends RuntimeException {
    private final String code;

    public TeachingFacadeException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
