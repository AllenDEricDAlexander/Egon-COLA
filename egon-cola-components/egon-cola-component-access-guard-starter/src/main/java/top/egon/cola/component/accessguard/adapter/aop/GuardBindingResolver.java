package top.egon.cola.component.accessguard.adapter.aop;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import top.egon.cola.component.accessguard.api.AccessGuard;
import top.egon.cola.component.accessguard.api.AllowListGuard;
import top.egon.cola.component.accessguard.api.RateLimitGuard;
import top.egon.cola.component.accessguard.api.TimeLimitGuard;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class GuardBindingResolver {

    public Optional<GuardBinding> resolve(Method method, Class<?> targetClass) {
        Method specific = AopUtils.getMostSpecificMethod(method, targetClass);
        List<GuardBinding> methodBindings = bindings(specific);
        if (methodBindings.isEmpty() && !specific.equals(method)) {
            methodBindings = bindings(method);
        }
        if (!methodBindings.isEmpty()) {
            return single(methodBindings, specific.toGenericString());
        }
        return single(bindings(targetClass), targetClass.getName());
    }

    private static List<GuardBinding> bindings(AnnotatedElement element) {
        List<GuardBinding> bindings = new ArrayList<>();
        AccessGuard access = AnnotatedElementUtils.findMergedAnnotation(element, AccessGuard.class);
        if (access != null) {
            bindings.add(new GuardBinding(access.value(), access.key(), GuardBinding.Kind.ACCESS));
        }
        AllowListGuard allow = AnnotatedElementUtils.findMergedAnnotation(element, AllowListGuard.class);
        if (allow != null) {
            bindings.add(new GuardBinding(allow.value(), allow.key(), GuardBinding.Kind.ALLOW_LIST));
        }
        RateLimitGuard rate = AnnotatedElementUtils.findMergedAnnotation(element, RateLimitGuard.class);
        if (rate != null) {
            bindings.add(new GuardBinding(rate.value(), rate.key(), GuardBinding.Kind.RATE_LIMIT));
        }
        TimeLimitGuard time = AnnotatedElementUtils.findMergedAnnotation(element, TimeLimitGuard.class);
        if (time != null) {
            bindings.add(new GuardBinding(time.value(), time.key(), GuardBinding.Kind.TIME_LIMIT));
        }
        return List.copyOf(bindings);
    }

    private static Optional<GuardBinding> single(List<GuardBinding> bindings, String source) {
        if (bindings.size() > 1) {
            throw new IllegalArgumentException("Multiple Access Guard bindings on " + source);
        }
        return bindings.stream().findFirst();
    }
}
