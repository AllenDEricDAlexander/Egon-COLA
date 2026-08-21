package top.egon.cola.component.rpc.consumer.proxy;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcDirectReference;
import top.egon.cola.component.rpc.annotation.EgonRpcReference;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceDefinition;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceDefinitionResolver;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceStrategy;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceStrategyFactory;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;

/** Resolves each annotated field once and installs one fixed-mode CGLIB proxy. */
public class EgonRpcReferenceBeanPostProcessor implements BeanPostProcessor {

    private final RpcContractValidator contractValidator;
    private final RpcReferenceDefinitionResolver definitionResolver;
    private final RpcReferenceStrategyFactory strategyFactory;
    private final RpcConsumerProxyFactory proxyFactory;

    private final boolean legacyMode;

    public EgonRpcReferenceBeanPostProcessor(
            RpcContractValidator contractValidator,
            RpcReferenceDefinitionResolver definitionResolver,
            RpcReferenceStrategyFactory strategyFactory,
            RpcConsumerProxyFactory proxyFactory) {
        this.contractValidator = java.util.Objects.requireNonNull(
                contractValidator,
                "contractValidator"
        );
        this.definitionResolver = java.util.Objects.requireNonNull(
                definitionResolver,
                "definitionResolver"
        );
        this.strategyFactory = java.util.Objects.requireNonNull(
                strategyFactory,
                "strategyFactory"
        );
        this.proxyFactory = java.util.Objects.requireNonNull(
                proxyFactory,
                "proxyFactory"
        );
        this.legacyMode = false;
    }

    /**
     * Compatibility constructor for programmatic clients that already own a
     * channel provider. New Spring-managed references use the fixed-mode
     * resolver/strategy constructor above.
     */
    public EgonRpcReferenceBeanPostProcessor(
            RpcConsumerProxyFactory proxyFactory) {
        this.contractValidator = new RpcContractValidator();
        this.definitionResolver = null;
        this.strategyFactory = null;
        this.proxyFactory = java.util.Objects.requireNonNull(
                proxyFactory,
                "proxyFactory"
        );
        this.legacyMode = true;
    }

    @Override
    public Object postProcessBeforeInitialization(
            Object bean,
            String beanName) throws BeansException {
        ReflectionUtils.doWithFields(
                bean.getClass(),
                field -> inject(bean, beanName, field),
                field -> field.isAnnotationPresent(EgonRpcReference.class)
                        || field.isAnnotationPresent(EgonRpcDirectReference.class)
        );
        return bean;
    }

    private void inject(
            Object bean,
            String beanName,
            java.lang.reflect.Field field) {
        EgonRpcReference reference = field.getAnnotation(EgonRpcReference.class);
        EgonRpcDirectReference direct = field.getAnnotation(
                EgonRpcDirectReference.class
        );
        if (reference != null && direct != null) {
            throw injectionFailure(
                    beanName,
                    field.getName(),
                    "@EgonRpcReference",
                    "cannot declare both @EgonRpcReference and "
                            + "@EgonRpcDirectReference"
            );
        }
        if (!field.getType().isInterface()) {
            throw injectionFailure(
                    beanName,
                    field.getName(),
                    annotationName(reference),
                    "field must be an interface"
            );
        }
        ReflectionUtils.makeAccessible(field);
        RpcContractDescriptor descriptor = contractValidator.validate(
                field.getType()
        );
        if (legacyMode) {
            long timeoutMs = reference == null || reference.timeoutMs() <= 0
                    ? -1 : reference.timeoutMs();
            try {
                ReflectionUtils.setField(
                        field,
                        bean,
                        proxyFactory.create(field.getType(), timeoutMs)
                );
                return;
            } catch (RuntimeException exception) {
                throw injectionFailure(
                        beanName,
                        field.getName(),
                        annotationName(reference),
                        exception.getMessage(),
                        exception
                );
            }
        }
        RpcReferenceDefinition definition;
        try {
            definition = definitionResolver.resolve(field, descriptor);
        } catch (RuntimeException exception) {
            throw injectionFailure(
                    beanName,
                    field.getName(),
                    annotationName(reference),
                    exception.getMessage(),
                    exception
            );
        }
        RpcReferenceStrategy strategy;
        try {
            strategy = strategyFactory.create(definition);
        } catch (RuntimeException exception) {
            throw injectionFailure(
                    beanName,
                    field.getName(),
                    annotationName(reference),
                    exception.getMessage(),
                    exception
            );
        }
        try {
            Object proxy = proxyFactory.create(
                    descriptor,
                    definition,
                    strategy
            );
            ReflectionUtils.setField(field, bean, proxy);
        } catch (RuntimeException exception) {
            strategy.close();
            throw injectionFailure(
                    beanName,
                    field.getName(),
                    annotationName(reference),
                    exception.getMessage(),
                    exception
            );
        }
    }

    private String annotationName(EgonRpcReference reference) {
        return reference == null
                ? "@EgonRpcDirectReference"
                : "@EgonRpcReference";
    }

    private IllegalStateException injectionFailure(
            String beanName,
            String fieldName,
            String annotation,
            String message) {
        return injectionFailure(
                beanName,
                fieldName,
                annotation,
                message,
                null
        );
    }

    private IllegalStateException injectionFailure(
            String beanName,
            String fieldName,
            String annotation,
            String message,
            Throwable cause) {
        String detail = message == null || message.isBlank()
                ? "reference resolution failed" : message;
        return new IllegalStateException(
                "RPC reference injection failed for bean '"
                        + beanName + "', field '" + fieldName + "': "
                        + annotation + " " + detail,
                cause
        );
    }
}
