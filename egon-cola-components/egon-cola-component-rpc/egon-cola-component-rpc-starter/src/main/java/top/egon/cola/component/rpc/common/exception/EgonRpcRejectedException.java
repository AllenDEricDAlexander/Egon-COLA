package top.egon.cola.component.rpc.common.exception;

import top.egon.cola.component.rpc.common.enums.EgonRpcErrorCode;

import java.io.Serial;

/**
 * A Provider-side rejection with a fixed typed code; callers keep seeing the
 * {@link EgonRpcException} contract.
 */
public class EgonRpcRejectedException extends EgonRpcException {

    @Serial
    private static final long serialVersionUID = 1L;

    public EgonRpcRejectedException(String message) {
        super(EgonRpcErrorCode.RPC_PROVIDER_REJECTED, message);
    }
}
