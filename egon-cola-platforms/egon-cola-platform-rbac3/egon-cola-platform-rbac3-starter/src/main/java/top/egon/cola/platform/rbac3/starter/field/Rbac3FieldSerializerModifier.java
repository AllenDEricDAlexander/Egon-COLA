package top.egon.cola.platform.rbac3.starter.field;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import top.egon.cola.component.common.desensitize.annotation.Sensitive;
import top.egon.cola.component.common.desensitize.annotation.SensitiveScene;
import top.egon.cola.component.common.desensitize.metadata.SensitiveMetadataResolver;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategyRegistry;
import top.egon.cola.platform.rbac3.starter.security.CurrentRbac3User;

import java.util.ArrayList;
import java.util.List;

final class Rbac3FieldSerializerModifier extends BeanSerializerModifier {

    private final CurrentRbac3User currentUser;
    private final SensitiveStrategyRegistry strategyRegistry;
    private final SensitiveMetadataResolver metadataResolver =
            new SensitiveMetadataResolver();

    Rbac3FieldSerializerModifier(
            CurrentRbac3User currentUser,
            SensitiveStrategyRegistry strategyRegistry) {
        this.currentUser = currentUser;
        this.strategyRegistry = strategyRegistry;
    }

    @Override
    public List<BeanPropertyWriter> changeProperties(
            SerializationConfig config,
            BeanDescription beanDesc,
            List<BeanPropertyWriter> beanProperties) {
        List<BeanPropertyWriter> result = new ArrayList<>(beanProperties.size());
        for (BeanPropertyWriter property : beanProperties) {
            RBACFieldResource resource = property.getAnnotation(RBACFieldResource.class);
            Sensitive staticSensitive = metadataResolver.resolve(
                    property.getAnnotation(Sensitive.class), SensitiveScene.RESPONSE)
                    .orElse(null);
            result.add(resource == null
                    ? property
                    : new Rbac3FieldPropertyWriter(
                    property, resource, staticSensitive, currentUser, strategyRegistry));
        }
        return result;
    }
}
