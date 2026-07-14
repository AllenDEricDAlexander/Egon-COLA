package top.egon.cola.component.methodextension.handler;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import top.egon.cola.component.methodextension.exception.MethodExtensionConfigurationException;

import java.util.List;
import java.util.Map;

public class MethodExtensionHandlerResolver {

    private final ListableBeanFactory beanFactory;

    public MethodExtensionHandlerResolver(ListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public MethodExtensionHandler resolve(Class<? extends MethodExtensionHandler> handlerType) {
        Map<String, MethodExtensionHandler> handlerBeans = beanFactory.getBeansOfType(MethodExtensionHandler.class);
        List<Map.Entry<String, MethodExtensionHandler>> matches = handlerBeans.entrySet().stream()
                .filter(entry -> matches(handlerType, entry.getValue()))
                .toList();
        if (matches.isEmpty()) {
            throw new MethodExtensionConfigurationException(
                    "No MethodExtensionHandler bean found for type " + handlerType.getName()
            );
        }
        if (matches.size() > 1) {
            String beanNames = matches.stream().map(Map.Entry::getKey).sorted().toList().toString();
            throw new MethodExtensionConfigurationException(
                    "Multiple MethodExtensionHandler beans found for type " + handlerType.getName()
                            + ": " + beanNames
            );
        }
        return matches.getFirst().getValue();
    }

    private boolean matches(Class<? extends MethodExtensionHandler> handlerType, MethodExtensionHandler handler) {
        return handlerType.isInstance(handler)
                || handlerType.isAssignableFrom(AopUtils.getTargetClass(handler));
    }
}
