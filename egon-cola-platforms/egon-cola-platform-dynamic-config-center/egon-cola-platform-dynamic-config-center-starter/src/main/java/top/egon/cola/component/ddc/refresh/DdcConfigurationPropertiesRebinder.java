package top.egon.cola.component.ddc.refresh;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.context.properties.ConfigurationPropertiesBean;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.boot.context.properties.bind.BindMethod;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import top.egon.cola.component.ddc.annotation.DdcRefreshable;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DdcConfigurationPropertiesRebinder
        implements SmartInitializingSingleton {

    private final ApplicationContext applicationContext;

    private final ConfigurationPropertiesBindingPostProcessor bindingPostProcessor;

    private final Environment environment;

    private volatile List<RefreshableBean> refreshableBeans = List.of();

    public DdcConfigurationPropertiesRebinder(
            ApplicationContext applicationContext,
            ConfigurationPropertiesBindingPostProcessor bindingPostProcessor,
            Environment environment) {
        this.applicationContext = applicationContext;
        this.bindingPostProcessor = bindingPostProcessor;
        this.environment = environment;
    }

    @Override
    public void afterSingletonsInstantiated() {
        List<RefreshableBean> candidates = new ArrayList<>();
        for (Map.Entry<String, ConfigurationPropertiesBean> entry
                : ConfigurationPropertiesBean.getAll(applicationContext).entrySet()) {
            ConfigurationPropertiesBean bean = entry.getValue();
            Class<?> beanType = ClassUtils.getUserClass(bean.getInstance());
            if (!AnnotatedElementUtils.hasAnnotation(
                    beanType,
                    DdcRefreshable.class
            )) {
                continue;
            }
            validate(entry.getKey(), bean, beanType);
            candidates.add(new RefreshableBean(
                    entry.getKey(),
                    bean.getInstance(),
                    ConfigurationPropertyName.of(
                            bean.getAnnotation().prefix()
                    )
            ));
        }
        refreshableBeans = List.copyOf(candidates);
    }

    public Set<String> rebind(Set<String> changedKeys,
                              Set<String> removedKeys) {
        Set<String> refreshedKeys = new LinkedHashSet<>();
        for (RefreshableBean bean : refreshableBeans) {
            Set<String> matchingKeys = matchingKeys(
                    bean.prefix(),
                    changedKeys
            );
            if (matchingKeys.isEmpty()
                    || hasRemovalWithoutFallback(
                            bean.prefix(),
                            removedKeys
                    )) {
                continue;
            }
            bindingPostProcessor.postProcessBeforeInitialization(
                    bean.instance(),
                    bean.name()
            );
            refreshedKeys.addAll(matchingKeys);
        }
        return Set.copyOf(refreshedKeys);
    }

    private void validate(String beanName,
                          ConfigurationPropertiesBean bean,
                          Class<?> beanType) {
        if (beanType.isRecord()
                || bean.asBindTarget().getBindMethod()
                == BindMethod.VALUE_OBJECT) {
            throw new IllegalStateException(
                    "DDC refreshable @ConfigurationProperties bean '"
                            + beanName + "' must use JavaBean binding"
            );
        }
        boolean writable = false;
        for (PropertyDescriptor descriptor
                : BeanUtils.getPropertyDescriptors(beanType)) {
            if (descriptor.getWriteMethod() != null) {
                writable = true;
                break;
            }
        }
        if (!writable) {
            throw new IllegalStateException(
                    "DDC refreshable @ConfigurationProperties bean '"
                            + beanName + "' must expose writable properties"
            );
        }
    }

    private boolean hasRemovalWithoutFallback(
            ConfigurationPropertyName prefix,
            Set<String> removedKeys) {
        for (String key : matchingKeys(prefix, removedKeys)) {
            if (environment.getProperty(key) == null) {
                return true;
            }
        }
        return false;
    }

    private Set<String> matchingKeys(
            ConfigurationPropertyName prefix,
            Set<String> keys) {
        Set<String> matching = new LinkedHashSet<>();
        for (String key : keys) {
            ConfigurationPropertyName name =
                    ConfigurationPropertyName.adapt(key, '.');
            if (prefix.isEmpty()
                    || prefix.equals(name)
                    || prefix.isAncestorOf(name)) {
                matching.add(key);
            }
        }
        return matching;
    }

    private record RefreshableBean(
            String name,
            Object instance,
            ConfigurationPropertyName prefix
    ) {
    }
}
