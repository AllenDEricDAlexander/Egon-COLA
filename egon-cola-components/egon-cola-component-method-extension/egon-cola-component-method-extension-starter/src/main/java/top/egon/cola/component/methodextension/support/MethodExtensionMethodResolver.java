package top.egon.cola.component.methodextension.support;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import top.egon.cola.component.methodextension.annotation.MethodExtension;
import top.egon.cola.component.methodextension.exception.MethodExtensionConfigurationException;

import java.lang.reflect.Method;
import java.util.Objects;

public class MethodExtensionMethodResolver {

    public ResolvedMethodExtension resolve(Method invokedMethod, Object target) {
        Objects.requireNonNull(invokedMethod, "invokedMethod must not be null");
        Objects.requireNonNull(target, "target must not be null");
        Class<?> targetClass = AopUtils.getTargetClass(target);
        Method specificMethod = AopUtils.getMostSpecificMethod(invokedMethod, targetClass);
        Method userMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
        MethodExtension annotation = AnnotatedElementUtils.findMergedAnnotation(userMethod, MethodExtension.class);
        if (annotation == null) {
            Method interfaceMethod = BridgeMethodResolver.findBridgedMethod(invokedMethod);
            annotation = AnnotatedElementUtils.findMergedAnnotation(interfaceMethod, MethodExtension.class);
        }
        if (annotation == null) {
            throw new MethodExtensionConfigurationException(
                    "No @MethodExtension found for " + userMethod.toGenericString()
            );
        }
        return new ResolvedMethodExtension(userMethod, annotation);
    }

    public record ResolvedMethodExtension(Method method, MethodExtension annotation) {
    }
}
