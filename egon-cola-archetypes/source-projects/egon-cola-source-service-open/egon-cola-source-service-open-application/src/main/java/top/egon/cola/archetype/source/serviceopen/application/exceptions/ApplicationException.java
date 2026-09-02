package top.egon.cola.archetype.source.serviceopen.application.exceptions;

public final class ApplicationException extends RuntimeException {

    private final ApplicationErrorCode code;

    public ApplicationException(ApplicationErrorCode code, String message) {
        this(code, message, null);
    }

    public ApplicationException(ApplicationErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ApplicationErrorCode code() { return code; }
}
