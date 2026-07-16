package top.egon.cola.component.methodextension.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.core.Ordered;
import top.egon.cola.component.methodextension.autoconfigure.MethodExtensionProperties;
import top.egon.cola.component.methodextension.event.NoopMethodExtensionEventPublisher;
import top.egon.cola.component.methodextension.execution.MethodExtensionExecutionResult;
import top.egon.cola.component.methodextension.execution.MethodExtensionExecutionService;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandlerResolver;
import top.egon.cola.component.methodextension.response.MethodExtensionResponseResolver;
import top.egon.cola.component.methodextension.support.MethodExtensionMethodResolver;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class MethodExtensionAop extends StaticMethodMatcherPointcutAdvisor implements Ordered {

    private final MethodExtensionProperties properties;

    private final MethodExtensionMethodResolver methodResolver;

    private final MethodExtensionExecutionService executionService;

    public MethodExtensionAop(
            MethodExtensionProperties properties,
            MethodExtensionMethodResolver methodResolver,
            MethodExtensionExecutionService executionService
    ) {
        this.properties = properties;
        this.methodResolver = methodResolver;
        this.executionService = executionService;
        setAdvice((MethodInterceptor) this::invoke);
    }

    public MethodExtensionAop(
            MethodExtensionProperties properties,
            MethodExtensionMethodResolver methodResolver,
            MethodExtensionHandlerResolver handlerResolver,
            MethodExtensionResponseResolver responseResolver
    ) {
        this(properties, methodResolver, new MethodExtensionExecutionService(
                methodResolver,
                handlerResolver,
                responseResolver,
                new NoopMethodExtensionEventPublisher()
        ));
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return Modifier.isPublic(method.getModifiers()) && methodResolver.matches(method, targetClass);
    }

    private Object invoke(MethodInvocation invocation) throws Throwable {
        MethodExtensionExecutionResult result = executionService.evaluate(
                invocation.getThis(), invocation.getMethod(), invocation.getArguments());
        return result.proceed() ? invocation.proceed() : result.rejectionValue();
    }

    @Override
    public int getOrder() {
        return properties.getOrder();
    }
}
