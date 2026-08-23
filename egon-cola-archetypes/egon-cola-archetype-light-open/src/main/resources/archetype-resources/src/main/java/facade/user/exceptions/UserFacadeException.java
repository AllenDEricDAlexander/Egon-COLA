package ${package}.facade.user.exceptions;

public class UserFacadeException extends RuntimeException {
    private final String code;

    public UserFacadeException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
