package top.egon.cola.platform.tianquan.jianshen.core.field;

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
