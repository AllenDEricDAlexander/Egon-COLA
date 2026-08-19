package top.egon.cola.component.outbox.aop;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.aop.support.AopUtils;
import top.egon.cola.component.outbox.exception.OutboxConfigurationException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

public class TransactionalMessageMethodValidator {

    private static final String REACTIVE_PUBLISHER = "org.reactivestreams.Publisher";

    private final String transactionManagerBeanName;

    public TransactionalMessageMethodValidator(String transactionManagerBeanName) {
        this.transactionManagerBeanName = transactionManagerBeanName;
    }

    public Method validate(Method method, Class<?> targetClass) {
        Method specificMethod = BridgeMethodResolver.findBridgedMethod(
                AopUtils.getMostSpecificMethod(method, targetClass)
        );
        int modifiers = specificMethod.getModifiers();
        if (!Modifier.isPublic(modifiers)
                || Modifier.isStatic(modifiers)
                || Modifier.isFinal(modifiers)
                || isAsynchronous(specificMethod.getReturnType())) {
            throw invalid();
        }

        Transactional transactional =
                AnnotatedElementUtils.findMergedAnnotation(specificMethod, Transactional.class);
        if (transactional == null) {
            transactional = AnnotatedElementUtils.findMergedAnnotation(
                    targetClass,
                    Transactional.class
            );
        }
        if (transactional != null) {
            validateTransaction(transactional);
        }
        return specificMethod;
    }

    private boolean isAsynchronous(Class<?> returnType) {
        if (Future.class.isAssignableFrom(returnType)
                || CompletionStage.class.isAssignableFrom(returnType)) {
            return true;
        }
        if (!ClassUtils.isPresent(REACTIVE_PUBLISHER, returnType.getClassLoader())) {
            return false;
        }
        Class<?> publisher = ClassUtils.resolveClassName(
                REACTIVE_PUBLISHER,
                returnType.getClassLoader()
        );
        return publisher.isAssignableFrom(returnType);
    }

    private void validateTransaction(Transactional transactional) {
        String selectedManager = transactional.transactionManager();
        if (transactional.readOnly()
                || transactional.propagation() != Propagation.REQUIRED
                || StringUtils.hasText(selectedManager)
                && !selectedManager.equals(transactionManagerBeanName)) {
            throw invalid();
        }
    }

    private OutboxConfigurationException invalid() {
        return new OutboxConfigurationException(
                "Invalid @TransactionalMessage method boundary"
        );
    }
}
