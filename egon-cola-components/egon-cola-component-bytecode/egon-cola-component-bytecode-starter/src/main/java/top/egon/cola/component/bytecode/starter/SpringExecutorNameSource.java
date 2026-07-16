package top.egon.cola.component.bytecode.starter;

import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Function;

final class SpringExecutorNameSource implements BeanPostProcessor, Function<Executor, String> {

    private final Map<Executor, String> names = Collections.synchronizedMap(new IdentityHashMap<>());

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof Executor executor) {
            names.put(executor, beanName);
        }
        return bean;
    }

    @Override
    public String apply(Executor executor) {
        return names.get(executor);
    }
}
