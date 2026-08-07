package top.egon.cola.component.common.desensitize.strategy;

import top.egon.cola.component.common.desensitize.annotation.SensitiveType;

public class IdCardSensitiveStrategy implements SensitiveStrategy {

    @Override
    public SensitiveType type() {
        return SensitiveType.ID_CARD;
    }

    @Override
    public String mask(String value) {
        return MaskingSupport.keepAround(value, 6, 4);
    }
}
