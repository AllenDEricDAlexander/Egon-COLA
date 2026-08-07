package top.egon.cola.component.common.desensitize.strategy;

import top.egon.cola.component.common.desensitize.annotation.SensitiveType;

public interface SensitiveStrategy {

    SensitiveType type();

    String mask(String value);
}
