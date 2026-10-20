package top.egon.cola.component.dtp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import top.egon.cola.component.dtp.executor.ManagedExecutorRegistry;
import top.egon.cola.component.dtp.executor.virtual.BoundedVirtualThreadExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @description 初始化后登记原执行器对象，避免收集全部 Bean 带来的创建循环
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DtpExecutorBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {

    @Qualifier("managedExecutorRegistry")
    private final ObjectProvider<ManagedExecutorRegistry> registry;

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) { this.beanFactory = beanFactory; }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof ThreadPoolExecutor || bean instanceof ThreadPoolTaskExecutor
                || bean instanceof BoundedVirtualThreadExecutor) {
            registry.getObject().registerBean(beanName, bean);
            if (beanFactory != null) {
                for (String alias : beanFactory.getAliases(beanName)) { registry.getObject().registerBean(alias, bean); }
            }
        }
        return bean;
    }
}
