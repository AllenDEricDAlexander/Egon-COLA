package top.egon.cola.component.methodextension.autoconfigure;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

final class MethodExtensionConditionSupport {

    private MethodExtensionConditionSupport() {
    }

    static MethodExtensionEngine effectiveEngine(Environment environment) {
        MethodExtensionProperties properties = Binder.get(environment)
                .bind("egon.cola.component.method-extension",
                        Bindable.of(MethodExtensionProperties.class))
                .orElseGet(MethodExtensionProperties::new);
        return properties.effectiveEngine();
    }
}
