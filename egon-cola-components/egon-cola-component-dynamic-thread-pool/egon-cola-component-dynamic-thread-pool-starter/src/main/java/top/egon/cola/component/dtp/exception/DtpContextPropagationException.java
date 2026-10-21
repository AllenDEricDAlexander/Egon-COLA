package top.egon.cola.component.dtp.exception;

import lombok.Getter;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.enums.ResultCode;
import java.io.Serial;

/** 上下文故障只保留阶段和声明键，不能携带上下文值。 */
@Getter
public class DtpContextPropagationException extends CommonException {
    @Serial
    private static final long serialVersionUID = 1L;
    private final String phase;
    private final String accessorKey;

    public DtpContextPropagationException(String phase, String accessorKey, Throwable cause) {
        super(ResultCode.SYSTEM_ERROR, cause);
        this.phase = phase;
        this.accessorKey = accessorKey;
    }
}
