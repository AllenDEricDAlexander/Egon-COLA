package top.egon.cola.platform.rbac3.starter.field;

import com.fasterxml.jackson.databind.module.SimpleModule;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategyRegistry;
import top.egon.cola.platform.rbac3.starter.security.CurrentRbac3User;

/** Jackson response module for RBAC3 field decisions. */
public class Rbac3FieldJacksonModule extends SimpleModule {

    public Rbac3FieldJacksonModule(
            CurrentRbac3User currentUser,
            SensitiveStrategyRegistry strategyRegistry) {
        super("Rbac3FieldJacksonModule");
        setSerializerModifier(new Rbac3FieldSerializerModifier(
                currentUser, strategyRegistry));
    }
}
