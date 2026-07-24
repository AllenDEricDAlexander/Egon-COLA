package top.egon.cola.component.rpc.provider;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.contract.RpcContractValidator;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RpcProviderBeanScanner {

    private final ApplicationContext applicationContext;

    private final RpcContractValidator contractValidator;

    public RpcProviderBeanScanner(ApplicationContext applicationContext,
                                  RpcContractValidator contractValidator) {
        this.applicationContext = applicationContext;
        this.contractValidator = contractValidator;
    }

    public RpcProviderMethodRegistry scan() {
        List<RpcProviderBinding> providers = new ArrayList<>();
        applicationContext.getBeansWithAnnotation(EgonRpcProvider.class)
                .values()
                .forEach(bean -> scanBean(bean, providers));
        return new RpcProviderMethodRegistry(providers);
    }

    private void scanBean(Object bean, List<RpcProviderBinding> providers) {
        Class<?> beanType = AopUtils.getTargetClass(bean);
        List<Class<?>> contracts = Arrays.stream(beanType.getInterfaces())
                .filter(type -> AnnotatedElementUtils.hasAnnotation(
                        type,
                        EgonRpcService.class
                ))
                .toList();
        if (contracts.isEmpty()) {
            throw new EgonRpcException(
                    EgonRpcErrorCode.RPC_INVALID_CONTRACT,
                    "@EgonRpcProvider bean implements no @EgonRpcService interface"
            );
        }
        contracts.forEach(contract -> providers.add(
                new RpcProviderBinding(bean, contractValidator.validate(contract))
        ));
    }
}
