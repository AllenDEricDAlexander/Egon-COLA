package top.egon.cola.component.common.desensitize.strategy.impl;

import top.egon.cola.component.common.desensitize.annotation.SensitiveType;
import top.egon.cola.component.common.desensitize.strategy.MaskingSupport;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategy;

public class AddressSensitiveStrategy implements SensitiveStrategy {

    private static final int KEEP_START = 6;

    @Override
    public SensitiveType type() {
        return SensitiveType.ADDRESS;
    }

    @Override
    public String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        int length = value.codePointCount(0, value.length());
        return MaskingSupport.keepStart(value, length > KEEP_START ? KEEP_START : 1);
    }
}
