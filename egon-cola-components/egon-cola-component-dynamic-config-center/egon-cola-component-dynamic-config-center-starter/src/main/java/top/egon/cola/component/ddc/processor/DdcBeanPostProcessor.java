package top.egon.cola.component.ddc.processor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ClassUtils;
import top.egon.cola.component.ddc.service.DdcFieldBindingService;

public class DdcBeanPostProcessor implements BeanPostProcessor {

    private final DdcFieldBindingService fieldBindingService;

    public DdcBeanPostProcessor(DdcFieldBindingService fieldBindingService) {
        this.fieldBindingService = fieldBindingService;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        fieldBindingService.bind(bean, ClassUtils.getUserClass(bean));
        return bean;
    }
}
