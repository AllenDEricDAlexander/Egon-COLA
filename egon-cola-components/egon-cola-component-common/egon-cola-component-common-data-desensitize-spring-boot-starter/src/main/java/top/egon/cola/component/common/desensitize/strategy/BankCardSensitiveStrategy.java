package top.egon.cola.component.common.desensitize.strategy;

import top.egon.cola.component.common.desensitize.annotation.SensitiveType;

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
