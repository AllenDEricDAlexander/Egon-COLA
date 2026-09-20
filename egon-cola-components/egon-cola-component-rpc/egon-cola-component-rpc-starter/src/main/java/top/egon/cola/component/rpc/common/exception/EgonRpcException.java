package top.egon.cola.component.rpc.common.exception;

import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.rpc.common.enums.EgonRpcErrorCode;

import java.io.Serial;

/**
 * Technical RPC failure. The stable typed code is carried by the common
 * {@code status} contract, and the enum identity stays available through
 * {@link #getRpcErrorCode()} because the inherited {@code getCode()} is int.
 */
public class EgonRpcException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final EgonRpcErrorCode rpcErrorCode;

    public EgonRpcException(EgonRpcErrorCode rpcErrorCode, String message) {
        super(ResultCode.SYSTEM_ERROR.getCode(), rpcErrorCode.name(), message);
        this.rpcErrorCode = rpcErrorCode;
    }

    public EgonRpcException(EgonRpcErrorCode rpcErrorCode, String message, Throwable cause) {
        super(ResultCode.SYSTEM_ERROR.getCode(), rpcErrorCode.name(), message, cause);
        this.rpcErrorCode = rpcErrorCode;
    }

    public EgonRpcErrorCode getRpcErrorCode() {
        return rpcErrorCode;
    }
}
