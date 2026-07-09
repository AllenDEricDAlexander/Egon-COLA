package ${package}.application.teaching.manage;

public class TeachingUseCaseException extends RuntimeException {
    private final String code;

    public TeachingUseCaseException(String code, String message) {
        super(message);
        this.code = code;
    }

    public TeachingUseCaseException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
