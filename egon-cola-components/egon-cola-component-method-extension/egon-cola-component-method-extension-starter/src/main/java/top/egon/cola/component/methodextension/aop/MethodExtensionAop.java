package top.egon.cola.component.methodextension.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import top.egon.cola.component.methodextension.autoconfigure.MethodExtensionProperties;
import top.egon.cola.component.methodextension.context.MethodExtensionContext;
import top.egon.cola.component.methodextension.exception.MethodExtensionConfigurationException;
import top.egon.cola.component.methodextension.exception.MethodExtensionException;
import top.egon.cola.component.methodextension.handler.MethodExtensionDecision;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandler;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandlerResolver;
import top.egon.cola.component.methodextension.response.MethodExtensionResponseResolver;
import top.egon.cola.component.methodextension.support.MethodExtensionMethodResolver;

import java.lang.reflect.Method;

@Aspect
public class MethodExtensionAop implements Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodExtensionAop.class);

    private final MethodExtensionProperties properties;

    private final MethodExtensionMethodResolver methodResolver;

    private final MethodExtensionHandlerResolver handlerResolver;

    private final MethodExtensionResponseResolver responseResolver;

    public MethodExtensionAop(
            MethodExtensionProperties properties,
            MethodExtensionMethodResolver methodResolver,
            MethodExtensionHandlerResolver handlerResolver,
            MethodExtensionResponseResolver responseResolver
    ) {
        this.properties = properties;
        this.methodResolver = methodResolver;
        this.handlerResolver = handlerResolver;
        this.responseResolver = responseResolver;
    }

    @Around("execution(public * *(..)) && @annotation(top.egon.cola.component.methodextension.annotation.MethodExtension)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method invokedMethod = ((MethodSignature) joinPoint.getSignature()).getMethod();
        MethodExtensionMethodResolver.ResolvedMethodExtension resolved;
        MethodExtensionHandler handler;
        try {
            resolved = methodResolver.resolve(invokedMethod, joinPoint.getTarget());
            handler = handlerResolver.resolve(resolved.annotation().handler());
        } catch (MethodExtensionException exception) {
            LOGGER.error("Invalid method extension configuration for {}", invokedMethod.toGenericString(), exception);
            throw exception;
        }

        Method method = resolved.method();
        Class<? extends MethodExtensionHandler> handlerType = resolved.annotation().handler();
        LOGGER.debug("Matched method extension {} with handler {}", method.toGenericString(), handlerType.getName());
        MethodExtensionDecision decision;
        try {
            LOGGER.debug("Executing method extension handler {}", handlerType.getName());
            decision = handler.evaluate(new MethodExtensionContext(joinPoint.getTarget(), method, joinPoint.getArgs()));
        } catch (Throwable exception) {
            LOGGER.error(
                    "Method extension handler {} failed for {}",
                    handlerType.getName(),
                    method.toGenericString(),
                    exception
            );
            throw exception;
        }
        if (decision == null) {
            MethodExtensionConfigurationException exception = new MethodExtensionConfigurationException(
                    "MethodExtensionHandler " + handlerType.getName() + " returned null for " + method.toGenericString()
            );
            LOGGER.error("Invalid method extension decision for {}", method.toGenericString(), exception);
            throw exception;
        }
        if (decision.allowed()) {
            LOGGER.debug("Method extension allowed {}", method.toGenericString());
            return joinPoint.proceed();
        }

        LOGGER.info(
                "Method extension rejected {} with handler {} and reason [{}]",
                method.toGenericString(),
                handlerType.getName(),
                decision.reason()
        );
        try {
            return responseResolver.resolve(method, decision, resolved.annotation().returnJson());
        } catch (MethodExtensionException exception) {
            LOGGER.error(
                    "Invalid method extension response for {}: {}",
                    method.toGenericString(),
                    exception.getMessage()
            );
            throw exception;
        }
    }

    @Override
    public int getOrder() {
        return properties.getOrder();
    }
}
