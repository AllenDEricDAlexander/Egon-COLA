package ${package}.application.user.manage;

public class UserUseCaseException extends RuntimeException {
    private final String code;

    public UserUseCaseException(String code, String message) {
        super(message);
        this.code = code;
    }

    public UserUseCaseException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
