package top.egon.cola.component.common.mybatis.exception;

import java.util.Objects;

/**
 * Stable startup failure for an invalid Egon COLA MyBatis-Plus contract.
 */
public final class EgonColaMybatisPlusConfigurationException extends IllegalStateException {

    private final String code;

    public EgonColaMybatisPlusConfigurationException(String code) {
        this(code, null);
    }

    public EgonColaMybatisPlusConfigurationException(String code, Throwable cause) {
        super(requireCode(code), cause);
        this.code = code;
    }

    public String getCode() {
        return code;
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
