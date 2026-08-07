package top.egon.cola.component.common.desensitize.strategy.impl;

import top.egon.cola.component.common.desensitize.annotation.SensitiveType;
import top.egon.cola.component.common.desensitize.strategy.MaskingSupport;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategy;

public class BankCardSensitiveStrategy implements SensitiveStrategy {

    @Override
    public SensitiveType type() {
        return SensitiveType.BANK_CARD;
    }

    @Override
    public String mask(String value) {
        return MaskingSupport.keepAround(value, 4, 4);
    }
}
