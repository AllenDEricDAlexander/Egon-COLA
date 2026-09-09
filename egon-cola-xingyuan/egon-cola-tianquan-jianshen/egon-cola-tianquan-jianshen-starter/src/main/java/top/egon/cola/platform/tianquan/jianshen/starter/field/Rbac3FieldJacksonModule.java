package top.egon.cola.platform.tianquan.jianshen.starter.field;

import com.fasterxml.jackson.databind.module.SimpleModule;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategyRegistry;
import top.egon.cola.platform.tianquan.jianshen.starter.security.CurrentRbac3User;

/** Jackson response module for Tianquan-Jianshen field decisions. */
public class Rbac3FieldJacksonModule extends SimpleModule {

    public Rbac3FieldJacksonModule(
            CurrentRbac3User currentUser,
            SensitiveStrategyRegistry strategyRegistry) {
        super("Rbac3FieldJacksonModule");
        setSerializerModifier(new Rbac3FieldSerializerModifier(
                currentUser, strategyRegistry));
    }
}
