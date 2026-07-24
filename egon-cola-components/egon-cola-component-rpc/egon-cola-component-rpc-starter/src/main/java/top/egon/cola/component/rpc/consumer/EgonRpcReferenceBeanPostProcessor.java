package top.egon.cola.component.rpc.consumer;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcReference;

public class EgonRpcReferenceBeanPostProcessor implements BeanPostProcessor {

    private final RpcConsumerProxyFactory proxyFactory;

    public EgonRpcReferenceBeanPostProcessor(
            RpcConsumerProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
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
                    if (!field.getType().isInterface()) {
                        throw new IllegalStateException(
                                "@EgonRpcReference field must be an interface"
                        );
                    }
                    ReflectionUtils.makeAccessible(field);
                    ReflectionUtils.setField(
                            field,
                            bean,
                            proxyFactory.create(
                                    field.getType(),
                                    reference.timeoutMs()
                            )
                    );
                },
                field -> field.isAnnotationPresent(EgonRpcReference.class)
        );
        return bean;
    }
}
