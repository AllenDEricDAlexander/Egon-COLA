package top.egon.cola.component.common.desensitize.strategy.impl;

import top.egon.cola.component.common.desensitize.annotation.SensitiveType;
import top.egon.cola.component.common.desensitize.strategy.MaskingSupport;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategy;

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
