package top.egon.cola.component.accessguard.adapter.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.core.Ordered;
import top.egon.cola.component.accessguard.core.GuardEngine;

import java.lang.reflect.Method;
import java.util.Objects;

public final class SpringAopAccessGuardAdvisor extends StaticMethodMatcherPointcutAdvisor
        implements MethodInterceptor {

    private final GuardBindingResolver bindingResolver;
    private final GuardEngine engine;

    public SpringAopAccessGuardAdvisor(GuardBindingResolver bindingResolver, GuardEngine engine) {
        this.bindingResolver = Objects.requireNonNull(bindingResolver, "bindingResolver");
        this.engine = Objects.requireNonNull(engine, "engine");
        setAdvice(this);
        setOrder(Ordered.HIGHEST_PRECEDENCE + 100);
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return bindingResolver.resolve(method, targetClass).isPresent();
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object target = invocation.getThis();
        Class<?> targetClass = target == null
                ? invocation.getMethod().getDeclaringClass()
                : AopUtils.getTargetClass(target);
        GuardBinding binding = bindingResolver.resolve(invocation.getMethod(), targetClass)
                .orElseThrow(() -> new IllegalStateException("Access Guard binding disappeared after proxy matching"));
        return engine.execute(SpringAopGuardInvocation.create(invocation, binding));
    }
}
