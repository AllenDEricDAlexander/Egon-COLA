package top.egon.cola.component.common.desensitize.strategy;

import top.egon.cola.component.common.desensitize.annotation.SensitiveType;

public class FullSensitiveStrategy implements SensitiveStrategy {

    @Override
    public SensitiveType type() {
        return SensitiveType.FULL;
    }

    @Override
    public String mask(String value) {
        return MaskingSupport.full(value);
    }
}
