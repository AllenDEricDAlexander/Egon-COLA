package top.egon.cola.component.common.desensitize.jackson;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import top.egon.cola.component.common.desensitize.annotation.Sensitive;
import top.egon.cola.component.common.desensitize.annotation.SensitiveScene;
import top.egon.cola.component.common.desensitize.metadata.SensitiveMetadataResolver;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategyRegistry;

import java.util.ArrayList;
import java.util.List;

final class SensitiveSerializerModifier extends BeanSerializerModifier {

    private final SensitiveStrategyRegistry strategyRegistry;

    private final SensitiveMetadataResolver metadataResolver;

    SensitiveSerializerModifier(SensitiveStrategyRegistry strategyRegistry,
                                SensitiveMetadataResolver metadataResolver) {
        this.strategyRegistry = strategyRegistry;
        this.metadataResolver = metadataResolver;
    }

    @Override
    public List<BeanPropertyWriter> changeProperties(
            SerializationConfig config,
            BeanDescription beanDesc,
            List<BeanPropertyWriter> beanProperties) {
        List<BeanPropertyWriter> result = new ArrayList<>(beanProperties.size());
        for (BeanPropertyWriter property : beanProperties) {
            Sensitive sensitive = metadataResolver.resolve(
                            property.getAnnotation(Sensitive.class),
                            SensitiveScene.RESPONSE
                    )
                    .orElse(null);
            if (sensitive == null) {
                result.add(property);
                continue;
            }
            if (!String.class.isAssignableFrom(property.getType().getRawClass())) {
                throw new IllegalStateException(
                        "@Sensitive must target a String property: "
                                + beanDesc.getBeanClass().getName()
                                + "." + property.getName()
                );
            }
            result.add(new SensitivePropertyWriter(
                    property,
                    sensitive.type(),
                    strategyRegistry
            ));
        }
        return result;
    }
}
