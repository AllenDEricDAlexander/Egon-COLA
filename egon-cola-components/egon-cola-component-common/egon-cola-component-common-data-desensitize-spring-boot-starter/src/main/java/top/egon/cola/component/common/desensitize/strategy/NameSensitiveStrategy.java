package top.egon.cola.component.common.desensitize.strategy;

import top.egon.cola.component.common.desensitize.annotation.SensitiveType;

public class NameSensitiveStrategy implements SensitiveStrategy {

    @Override
    public SensitiveType type() {
        return SensitiveType.NAME;
    }

    @Override
    public String mask(String value) {
        return MaskingSupport.keepStart(value, 1);
    }
}
