package top.egon.cola.component.rpc.consumer.proxy;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcDirectReference;
import top.egon.cola.component.rpc.annotation.EgonRpcReference;
import top.egon.cola.component.rpc.consumer.gateway.RpcConsumerGatewayManager;

public class EgonRpcReferenceBeanPostProcessor implements BeanPostProcessor {

    private final RpcConsumerProxyFactory gatewayProxyFactory;

    private final RpcConsumerGatewayManager gatewayManager;

    private final RpcDirectReferenceProxyFactory directProxyFactory;

    public EgonRpcReferenceBeanPostProcessor(
            RpcConsumerProxyFactory proxyFactory) {
        this(proxyFactory, null, null);
    }

    public EgonRpcReferenceBeanPostProcessor(
            RpcConsumerProxyFactory gatewayProxyFactory,
            RpcConsumerGatewayManager gatewayManager,
            RpcDirectReferenceProxyFactory directProxyFactory) {
        this.gatewayProxyFactory = gatewayProxyFactory;
        this.gatewayManager = gatewayManager;
        this.directProxyFactory = directProxyFactory;
    }

    @Override
    public Object postProcessBeforeInitialization(
            Object bean,
            String beanName) throws BeansException {
        ReflectionUtils.doWithFields(
                bean.getClass(),
                field -> {
                    EgonRpcReference reference =
                            field.getAnnotation(EgonRpcReference.class);
                    EgonRpcDirectReference directReference =
                            field.getAnnotation(
                                    EgonRpcDirectReference.class
                            );
                    if (reference != null && directReference != null) {
                        throw injectionFailure(
                                beanName,
                                field.getName(),
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
                    if (reference != null) {
                        injectGateway(
                                bean,
                                beanName,
                                field,
                                reference
                        );
                    } else {
                        injectDirect(
                                bean,
                                beanName,
                                field,
                                directReference
                        );
                    }
                },
                field -> field.isAnnotationPresent(EgonRpcReference.class)
                        || field.isAnnotationPresent(
                                EgonRpcDirectReference.class
                        )
        );
        return bean;
    }

    private void injectGateway(
            Object bean,
            String beanName,
            java.lang.reflect.Field field,
            EgonRpcReference reference) {
        if (gatewayProxyFactory == null) {
            throw injectionFailure(
                    beanName,
                    field.getName(),
                    "@EgonRpcReference",
                    "requires an RPC Gateway directory"
            );
        }
        if (gatewayManager != null) {
            gatewayManager.registerDemand();
        }
        ReflectionUtils.setField(
                field,
                bean,
                gatewayProxyFactory.create(
                        field.getType(),
                        reference.timeoutMs()
                )
        );
    }

    private void injectDirect(
            Object bean,
            String beanName,
            java.lang.reflect.Field field,
            EgonRpcDirectReference reference) {
        if (directProxyFactory == null) {
            throw injectionFailure(
                    beanName,
                    field.getName(),
                    "@EgonRpcDirectReference",
                    "requires an RPC Provider directory"
            );
        }
        ReflectionUtils.setField(
                field,
                bean,
                directProxyFactory.create(field.getType(), reference)
        );
    }

    private String annotationName(EgonRpcReference reference) {
        return reference == null
                ? "@EgonRpcDirectReference"
                : "@EgonRpcReference";
    }

    private IllegalStateException injectionFailure(
            String beanName,
            String fieldName,
            String message) {
        return new IllegalStateException(
                "RPC reference injection failed for bean '"
                        + beanName + "', field '" + fieldName + "': "
                        + message
        );
    }

    private IllegalStateException injectionFailure(
            String beanName,
            String fieldName,
            String annotation,
            String message) {
        return injectionFailure(
                beanName,
                fieldName,
                annotation + " " + message
        );
    }
}
