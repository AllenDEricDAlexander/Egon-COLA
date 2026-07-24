package top.egon.cola.component.rpc.exception;

public class EgonRpcException extends RuntimeException {

    private final EgonRpcErrorCode code;

    public EgonRpcException(EgonRpcErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public EgonRpcException(EgonRpcErrorCode code,
                            String message,
                            Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public EgonRpcErrorCode getCode() {
        return code;
    }
}
