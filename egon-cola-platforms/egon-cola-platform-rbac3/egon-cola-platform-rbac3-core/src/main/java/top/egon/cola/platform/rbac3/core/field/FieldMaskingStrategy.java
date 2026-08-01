package top.egon.cola.platform.rbac3.core.field;

public interface FieldMaskingStrategy {

    MaskingType type();

    String mask(String value);

    enum MaskingType {
        FIXED,
        EMAIL,
        PHONE,
        BANK_ACCOUNT
    }
}
