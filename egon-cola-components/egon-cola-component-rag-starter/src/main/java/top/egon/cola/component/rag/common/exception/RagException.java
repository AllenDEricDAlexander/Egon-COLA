package top.egon.cola.component.rag.common.exception;

import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;

import java.io.Serial;

/**
 * Base type for every failure raised by the RAG starter.
 *
 * <p>Every instance carries a stable machine-readable status and a {@code safeMessage} that
 * never contains document content, chunk text, vectors, provider payloads, endpoints or keys.
 */
public class RagException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RagException(String code, String safeMessage) {
        super(ResultCode.SYSTEM_ERROR.getCode(), code, safeMessage);
    }

    public RagException(String code, String safeMessage, Throwable cause) {
        super(ResultCode.SYSTEM_ERROR.getCode(), code, safeMessage, cause);
    }

    public String safeMessage() {
        return getMessage();
    }
}
