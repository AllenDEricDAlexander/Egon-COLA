package top.egon.cola.component.common.desensitize.strategy.impl;

import top.egon.cola.component.common.desensitize.annotation.SensitiveType;
import top.egon.cola.component.common.desensitize.strategy.MaskingSupport;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategy;

public class EmailSensitiveStrategy implements SensitiveStrategy {

    @Override
    public SensitiveType type() {
        return SensitiveType.EMAIL;
    }

    @Override
    public String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        int atIndex = value.indexOf('@');
        if (atIndex <= 0) {
            return MaskingSupport.full(value);
        }
        String localPart = value.substring(0, atIndex);
        return MaskingSupport.keepStart(localPart, 1) + value.substring(atIndex);
    }
}
