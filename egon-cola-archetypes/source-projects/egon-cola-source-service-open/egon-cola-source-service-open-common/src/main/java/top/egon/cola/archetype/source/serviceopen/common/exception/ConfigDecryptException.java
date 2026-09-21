package top.egon.cola.archetype.source.serviceopen.common.exception;

import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;

import java.io.Serial;

/** Technical failure while resolving an encrypted configuration value. */
public class ConfigDecryptException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String STATUS = "CONFIG_DECRYPT_FAILED";

    public ConfigDecryptException(String message) {
        super(ResultCode.SYSTEM_ERROR.getCode(), STATUS, message);
    }

    public ConfigDecryptException(String message, Throwable cause) {
        super(ResultCode.SYSTEM_ERROR.getCode(), STATUS, message, cause);
    }
}
