package top.egon.cola.component.ddc.common;

public class DdcException extends RuntimeException {

    public DdcException(String message) {
        super(message);
    }

    public DdcException(String message, Throwable cause) {
        super(message, cause);
    }
}
