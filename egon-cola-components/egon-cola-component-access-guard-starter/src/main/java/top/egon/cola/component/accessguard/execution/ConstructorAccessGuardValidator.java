package top.egon.cola.component.accessguard.execution;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.StringUtils;
import top.egon.cola.component.accessguard.annotation.AccessGuard;
import top.egon.cola.component.accessguard.annotation.FailStrategy;
import top.egon.cola.component.accessguard.config.AccessGuardRule;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public class ConstructorAccessGuardValidator {

    private final ParameterNameDiscoverer parameterNames = new DefaultParameterNameDiscoverer();

    public void validate(Constructor<?> constructor, AccessGuardRule rule) {
        if (constructor.getAnnotation(AccessGuard.class) == null) {
            throw unsupported(constructor, "only aggregate @AccessGuard is supported");
        }
        int modifiers = constructor.getModifiers();
        if (!Modifier.isPublic(modifiers) && !Modifier.isPrivate(modifiers)) {
            throw unsupported(constructor, "only public and private constructors are supported");
        }
        if (rule.timeoutEnabled()) {
            throw unsupported(constructor, "timeout is not supported");
        }
        if (StringUtils.hasText(rule.fallbackMethod())) {
            throw unsupported(constructor, "fallbackMethod is not supported");
        }
        if (StringUtils.hasText(rule.returnJson())) {
            throw unsupported(constructor, "returnJson is not supported");
        }
        if (rule.failStrategy() == FailStrategy.LOCAL_FALLBACK) {
            throw unsupported(constructor, "LOCAL_FALLBACK is not supported");
        }
        validateKey(constructor, StringUtils.hasText(rule.keyExpression())
                ? rule.keyExpression() : rule.key());
    }

    private void validateKey(Constructor<?> constructor, String configuredKey) {
        String key = configuredKey == null ? "" : configuredKey.trim();
        if (key.isEmpty() || key.equalsIgnoreCase("all") || key.equalsIgnoreCase("ip")
                || key.startsWith("header:")) {
            return;
        }
        if (key.equals("this") || key.startsWith("this.")
                || key.equals("target") || key.startsWith("target.")) {
            throw unsupported(constructor, "instance state key expressions are not supported");
        }
        String[] names = parameterNames.getParameterNames(constructor);
        if (names == null || names.length == 0) {
            if (constructor.getParameterCount() == 1) {
                return;
            }
            throw unsupported(constructor, "key must resolve from constructor parameters");
        }
        boolean parameterPath = Arrays.stream(names)
                .anyMatch(name -> key.equals(name) || key.startsWith(name + "."));
        if (!parameterPath && constructor.getParameterCount() != 1) {
            throw unsupported(constructor, "key must resolve from constructor parameters");
        }
    }

    private IllegalArgumentException unsupported(Constructor<?> constructor, String reason) {
        return new IllegalArgumentException(
                "Unsupported Access Guard constructor " + constructor.toGenericString() + ": " + reason);
    }
}
