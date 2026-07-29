package top.egon.cola.component.accessguard.adapter.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.core.Ordered;
import top.egon.cola.component.accessguard.core.GuardEngine;
import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.execution.async.CompletionStageGuardExecutor;
import top.egon.cola.component.accessguard.execution.reactive.ReactiveGuardExecutor;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

public final class SpringAopAccessGuardAdvisor extends StaticMethodMatcherPointcutAdvisor
        implements MethodInterceptor {

    private final GuardBindingResolver bindingResolver;
    private final GuardEngine engine;
    private final CompletionStageGuardExecutor completionStageExecutor;
    private final ReactiveGuardExecutor reactiveExecutor;

    public SpringAopAccessGuardAdvisor(GuardBindingResolver bindingResolver, GuardEngine engine) {
        this(bindingResolver, engine, new CompletionStageGuardExecutor(engine), null);
    }

    public SpringAopAccessGuardAdvisor(
            GuardBindingResolver bindingResolver,
            GuardEngine engine,
            CompletionStageGuardExecutor completionStageExecutor,
            ReactiveGuardExecutor reactiveExecutor
    ) {
        this.bindingResolver = Objects.requireNonNull(bindingResolver, "bindingResolver");
        this.engine = Objects.requireNonNull(engine, "engine");
        this.completionStageExecutor = Objects.requireNonNull(completionStageExecutor, "completionStageExecutor");
        this.reactiveExecutor = reactiveExecutor;
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
        GuardInvocation guardInvocation = SpringAopGuardInvocation.create(invocation, binding);
        Class<?> returnType = guardInvocation.executable() instanceof Method method
                ? method.getReturnType()
                : invocation.getMethod().getReturnType();
        if (CompletionStage.class.isAssignableFrom(returnType)) {
            return completionStageExecutor.guard(guardInvocation);
        }
        if (reactiveExecutor != null && reactiveExecutor.supports(returnType)) {
            return reactiveExecutor.guard(guardInvocation, returnType);
        }
        if (returnType.getName().startsWith("reactor.core.publisher.")) {
            throw new IllegalStateException("Reactive Access Guard method requires Reactor adapter");
        }
        return engine.execute(guardInvocation);
    }
}
