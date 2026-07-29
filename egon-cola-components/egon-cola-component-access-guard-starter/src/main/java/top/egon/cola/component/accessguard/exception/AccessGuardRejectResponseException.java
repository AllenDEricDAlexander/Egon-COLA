package top.egon.cola.component.accessguard.exception;

public class AccessGuardRejectResponseException extends RuntimeException {

    public AccessGuardRejectResponseException(String message) {
        super(message);
    }

    public AccessGuardRejectResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
