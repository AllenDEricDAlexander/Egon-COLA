package top.egon.cola.component.methodextension.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.methodextension.context.MethodExtensionContext;
import top.egon.cola.component.methodextension.event.MethodExtensionEvent;
import top.egon.cola.component.methodextension.event.MethodExtensionEventPublisher;
import top.egon.cola.component.methodextension.exception.MethodExtensionConfigurationException;
import top.egon.cola.component.methodextension.exception.MethodExtensionException;
import top.egon.cola.component.methodextension.handler.MethodExtensionDecision;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandler;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandlerResolver;
import top.egon.cola.component.methodextension.response.MethodExtensionResponseResolver;
import top.egon.cola.component.methodextension.support.MethodExtensionMethodResolver;

import java.lang.reflect.Method;

public class MethodExtensionExecutionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            MethodExtensionExecutionService.class);

    private final MethodExtensionMethodResolver methodResolver;
    private final MethodExtensionHandlerResolver handlerResolver;
    private final MethodExtensionResponseResolver responseResolver;
    private final MethodExtensionEventPublisher eventPublisher;

    public MethodExtensionExecutionService(
            MethodExtensionMethodResolver methodResolver,
            MethodExtensionHandlerResolver handlerResolver,
            MethodExtensionResponseResolver responseResolver,
            MethodExtensionEventPublisher eventPublisher
    ) {
        this.methodResolver = methodResolver;
        this.handlerResolver = handlerResolver;
        this.responseResolver = responseResolver;
        this.eventPublisher = eventPublisher;
    }

    public MethodExtensionExecutionResult evaluate(
            Object target,
            Method invokedMethod,
            Object[] arguments
    ) throws Throwable {
        MethodExtensionMethodResolver.ResolvedMethodExtension resolved;
        MethodExtensionHandler handler;
        try {
            resolved = methodResolver.resolve(invokedMethod, target);
            handler = handlerResolver.resolve(resolved.annotation().handler());
        } catch (MethodExtensionException exception) {
            LOGGER.error("Invalid method extension configuration for {}",
                    invokedMethod.toGenericString(), exception);
            publish(invokedMethod, null, "ERROR", exception.getClass().getName());
            throw exception;
        }

        Method method = resolved.method();
        Class<? extends MethodExtensionHandler> handlerType = resolved.annotation().handler();
        LOGGER.debug("Matched method extension {} with handler {}",
                method.toGenericString(), handlerType.getName());
        MethodExtensionDecision decision;
        try {
            LOGGER.debug("Executing method extension handler {}", handlerType.getName());
            decision = handler.evaluate(new MethodExtensionContext(target, method, arguments));
        } catch (Throwable exception) {
            LOGGER.error(
                    "Method extension handler {} failed for {}",
                    handlerType.getName(),
                    method.toGenericString(),
                    exception
            );
            publish(method, handlerType, "ERROR", exception.getClass().getName());
            throw exception;
        }
        if (decision == null) {
            MethodExtensionConfigurationException exception =
                    new MethodExtensionConfigurationException(
                            "MethodExtensionHandler " + handlerType.getName()
                                    + " returned null for " + method.toGenericString());
            LOGGER.error("Invalid method extension decision for {}",
                    method.toGenericString(), exception);
            publish(method, handlerType, "ERROR", exception.getClass().getName());
            throw exception;
        }
        if (decision.allowed()) {
            LOGGER.debug("Method extension allowed {}", method.toGenericString());
            publish(method, handlerType, "ALLOW", "");
            return MethodExtensionExecutionResult.proceedInvocation();
        }

        LOGGER.info(
                "Method extension rejected {} with handler {} and reason [{}]",
                method.toGenericString(),
                handlerType.getName(),
                decision.reason()
        );
        try {
            Object rejectionValue = responseResolver.resolve(
                    method, decision, resolved.annotation().returnJson());
            publish(method, handlerType, "REJECT", decision.reason());
            return MethodExtensionExecutionResult.reject(rejectionValue);
        } catch (MethodExtensionException exception) {
            LOGGER.error(
                    "Invalid method extension response for {}: {}",
                    method.toGenericString(),
                    exception.getMessage()
            );
            publish(method, handlerType, "ERROR", exception.getClass().getName());
            throw exception;
        }
    }

    private void publish(
            Method method,
            Class<? extends MethodExtensionHandler> handlerType,
            String outcome,
            String reason
    ) {
        try {
            eventPublisher.publish(new MethodExtensionEvent(
                    method.toGenericString(),
                    handlerType == null ? "" : handlerType.getName(),
                    outcome,
                    reason
            ));
        } catch (Throwable exception) {
            LOGGER.warn("Method extension event publisher failed for {}",
                    method.toGenericString(), exception);
        }
    }
}
