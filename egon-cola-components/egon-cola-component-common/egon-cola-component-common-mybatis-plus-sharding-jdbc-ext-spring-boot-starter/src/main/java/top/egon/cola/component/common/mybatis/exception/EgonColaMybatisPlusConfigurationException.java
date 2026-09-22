package top.egon.cola.component.common.mybatis.exception;

import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;

import java.io.Serial;
import java.util.Objects;

/**
 * Stable startup failure for an invalid Egon COLA MyBatis-Plus contract.
 */
public final class EgonColaMybatisPlusConfigurationException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public EgonColaMybatisPlusConfigurationException(String code) {
        this(code, null);
    }

    public EgonColaMybatisPlusConfigurationException(String code, Throwable cause) {
        super(ResultCode.SYSTEM_ERROR.getCode(), requireCode(code), requireCode(code), cause);
    }

    private static String requireCode(String code) {
        String checked = Objects.requireNonNull(code, "code must not be null");
        if (checked.isBlank() || !checked.chars().allMatch(value ->
                value == '_' || value >= 'A' && value <= 'Z' || value >= '0' && value <= '9')) {
            throw new IllegalArgumentException("code must be an uppercase identifier");
        }
        return checked;
    }
}
