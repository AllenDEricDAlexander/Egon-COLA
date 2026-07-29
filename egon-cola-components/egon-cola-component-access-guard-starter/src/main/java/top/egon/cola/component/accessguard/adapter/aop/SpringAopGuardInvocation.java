package top.egon.cola.component.accessguard.adapter.aop;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import top.egon.cola.component.accessguard.core.GuardEntryType;
import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.GuardInvocationKind;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SpringAopGuardInvocation {

    public static final String BINDING_KEY_ATTRIBUTE = "accessGuard.bindingKey";

    private SpringAopGuardInvocation() {
    }

    public static GuardInvocation create(MethodInvocation invocation, GuardBinding binding) {
        Object target = invocation.getThis();
        Class<?> targetClass = target == null ? invocation.getMethod().getDeclaringClass() : AopUtils.getTargetClass(target);
        Method method = AopUtils.getMostSpecificMethod(invocation.getMethod(), targetClass);
        Map<String, Object> attributes = new LinkedHashMap<>();
        if (!binding.key().isBlank()) {
            attributes.put(BINDING_KEY_ATTRIBUTE, binding.key());
        }
        return new GuardInvocation(
                binding.ruleId(),
                target,
                targetClass,
                method,
                invocation.getArguments(),
                attributes,
                GuardEntryType.AOP,
                GuardInvocationKind.METHOD,
                invocation::proceed);
    }
}
