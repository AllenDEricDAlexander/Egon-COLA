package top.egon.cola.component.methodextension.exception;

public class MethodExtensionException extends RuntimeException {

    public MethodExtensionException(String message) {
        super(message);
    }

    public MethodExtensionException(String message, Throwable cause) {
        super(message, cause);
    }
}
