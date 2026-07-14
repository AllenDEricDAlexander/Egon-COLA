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
        ResolvedMethodExtension resolved = find(invokedMethod, targetClass);
        if (resolved == null) {
            Method userMethod = BridgeMethodResolver.findBridgedMethod(
                    AopUtils.getMostSpecificMethod(invokedMethod, targetClass)
            );
            throw new MethodExtensionConfigurationException(
                    "No @MethodExtension found for " + userMethod.toGenericString()
            );
        }
        return resolved;
    }

    public boolean matches(Method invokedMethod, Class<?> targetClass) {
        Objects.requireNonNull(invokedMethod, "invokedMethod must not be null");
        Objects.requireNonNull(targetClass, "targetClass must not be null");
        return find(invokedMethod, targetClass) != null;
    }

    private ResolvedMethodExtension find(Method invokedMethod, Class<?> targetClass) {
        Method specificMethod = AopUtils.getMostSpecificMethod(invokedMethod, targetClass);
        Method userMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
        MethodExtension annotation = AnnotatedElementUtils.findMergedAnnotation(userMethod, MethodExtension.class);
        if (annotation == null) {
            Method interfaceMethod = BridgeMethodResolver.findBridgedMethod(invokedMethod);
            annotation = AnnotatedElementUtils.findMergedAnnotation(interfaceMethod, MethodExtension.class);
        }
        if (annotation == null) {
            return null;
        }
        return new ResolvedMethodExtension(userMethod, annotation);
    }

    public record ResolvedMethodExtension(Method method, MethodExtension annotation) {
    }
}
