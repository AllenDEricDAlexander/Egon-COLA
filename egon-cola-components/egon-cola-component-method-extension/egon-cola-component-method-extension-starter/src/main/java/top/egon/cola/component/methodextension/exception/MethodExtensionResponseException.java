package top.egon.cola.component.methodextension.exception;

public class MethodExtensionResponseException extends MethodExtensionException {

    public MethodExtensionResponseException(String message) {
        super(message);
    }

    public MethodExtensionResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
