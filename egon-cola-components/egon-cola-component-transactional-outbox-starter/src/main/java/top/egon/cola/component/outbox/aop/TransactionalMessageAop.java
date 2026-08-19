package top.egon.cola.component.outbox.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.outbox.annotation.TransactionalMessage;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties;

import java.lang.reflect.Method;

public class TransactionalMessageAop
        extends StaticMethodMatcherPointcutAdvisor
        implements Ordered {

    private final TransactionalOutbox transactionalOutbox;
    private final TransactionTemplate transactionTemplate;
    private final OutboxMessageExpressionResolver expressionResolver;
    private final TransactionalMessageMethodValidator methodValidator;
    private final TransactionalOutboxProperties properties;

    public TransactionalMessageAop(
            TransactionalOutbox transactionalOutbox,
            PlatformTransactionManager transactionManager,
            String transactionManagerBeanName,
            OutboxMessageExpressionResolver expressionResolver,
            TransactionalMessageMethodValidator methodValidator,
            TransactionalOutboxProperties properties
    ) {
        this.transactionalOutbox = transactionalOutbox;
        this.expressionResolver = expressionResolver;
        this.methodValidator = methodValidator;
        this.properties = properties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRED
        );
        setAdvice((MethodInterceptor) this::invokeWithinTransaction);
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        Method specificMethod = BridgeMethodResolver.findBridgedMethod(
                AopUtils.getMostSpecificMethod(method, targetClass)
        );
        TransactionalMessage annotation = AnnotatedElementUtils.findMergedAnnotation(
                specificMethod,
                TransactionalMessage.class
        );
        if (annotation == null) {
            return false;
        }
        methodValidator.validate(specificMethod, targetClass);
        return true;
    }

    @Override
    public int getOrder() {
        return properties.getAnnotation().getOrder();
    }

    private Object invokeWithinTransaction(MethodInvocation invocation) throws Throwable {
        Object[] resultHolder = new Object[1];
        Throwable[] failureHolder = new Throwable[1];
        try {
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    Object result = invocation.proceed();
                    TransactionalMessage annotation = findAnnotation(invocation);
                    OutboxMessage message = expressionResolver.resolve(
                            annotation.message(),
                            invocation.getMethod(),
                            invocation.getArguments(),
                            result
                    );
                    transactionalOutbox.enqueue(message);
                    resultHolder[0] = result;
                } catch (Throwable failure) {
                    failureHolder[0] = failure;
                    status.setRollbackOnly();
                }
            });
        } catch (Throwable completionFailure) {
            if (failureHolder[0] == null) {
                throw completionFailure;
            }
            failureHolder[0].addSuppressed(completionFailure);
        }
        if (failureHolder[0] != null) {
            throw failureHolder[0];
        }
        return resultHolder[0];
    }

    private TransactionalMessage findAnnotation(MethodInvocation invocation) {
        Object target = invocation.getThis();
        Class<?> targetClass = target == null
                ? invocation.getMethod().getDeclaringClass()
                : target.getClass();
        Method specificMethod = BridgeMethodResolver.findBridgedMethod(
                AopUtils.getMostSpecificMethod(invocation.getMethod(), targetClass)
        );
        return AnnotatedElementUtils.findMergedAnnotation(
                specificMethod,
                TransactionalMessage.class
        );
    }
}
