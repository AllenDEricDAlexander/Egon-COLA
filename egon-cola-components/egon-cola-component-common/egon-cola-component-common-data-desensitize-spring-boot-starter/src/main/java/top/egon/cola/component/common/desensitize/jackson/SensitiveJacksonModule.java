package top.egon.cola.component.common.desensitize.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import top.egon.cola.component.common.desensitize.metadata.SensitiveMetadataResolver;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategyRegistry;

public class SensitiveJacksonModule extends SimpleModule {

    public SensitiveJacksonModule(SensitiveStrategyRegistry strategyRegistry,
                                  SensitiveMetadataResolver metadataResolver) {
        super("SensitiveJacksonModule");
        setSerializerModifier(new SensitiveSerializerModifier(
                strategyRegistry,
                metadataResolver
        ));
    }
}
