package top.egon.cola.component.accessguard.exception;

public class AccessGuardRejectedException extends RuntimeException {

    public AccessGuardRejectedException(String message) {
        super(message);
    }

    public AccessGuardRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
