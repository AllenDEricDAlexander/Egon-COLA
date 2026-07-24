package top.egon.cola.component.rpc.exception;

public class EgonRpcRejectedException extends EgonRpcException {

    public EgonRpcRejectedException(String message) {
        super(EgonRpcErrorCode.RPC_PROVIDER_REJECTED, message);
    }
}
