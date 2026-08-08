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

/**
 * 发现并重新绑定允许动态刷新的 Spring Boot 配置属性 Bean。
 * Discovers and rebinds Spring Boot configuration-properties Beans that allow dynamic refresh.
 *
 * <p>仅使用 JavaBean 绑定且带有 {@link DdcRefreshable} 的配置属性 Bean 会被纳入刷新。
 * 被删除且没有其他属性源回退值的键不会触发重绑定，以避免把缺失值写入可变 Bean。</p>
 *
 * <p>Only configuration-properties Beans annotated with {@link DdcRefreshable} and using JavaBean binding
 * participate in refresh. A removed key without a fallback value from another property source prevents rebinding,
 * avoiding writes of missing values into mutable Beans.</p>
 */
public class DdcConfigurationPropertiesRebinder
        implements SmartInitializingSingleton {

    /**
     * 用于发现配置属性 Bean 的应用上下文。 Application context used to discover configuration-properties Beans.
     */
    private final ApplicationContext applicationContext;

    /**
     * 执行配置属性重新绑定的后处理器。 Post-processor that performs configuration-properties rebinding.
     */
    private final ConfigurationPropertiesBindingPostProcessor bindingPostProcessor;

    /**
     * 用于检查删除键是否存在回退值的环境。 Environment used to check fallback values for removed keys.
     */
    private final Environment environment;

    /**
     * 初始化完成后缓存的可刷新 Bean 不可变列表。 Immutable list of refreshable Beans cached after initialization.
     */
    private volatile List<RefreshableBean> refreshableBeans = List.of();

    /**
     * 创建配置属性重新绑定器。
     * Creates a configuration-properties rebinder.
     *
     * @param applicationContext   Spring 应用上下文; Spring application context
     * @param bindingPostProcessor 配置属性绑定后处理器; configuration-properties binding post-processor
     * @param environment          Spring 环境; Spring environment
     */
    public DdcConfigurationPropertiesRebinder(
            ApplicationContext applicationContext,
            ConfigurationPropertiesBindingPostProcessor bindingPostProcessor,
            Environment environment) {
        this.applicationContext = applicationContext;
        this.bindingPostProcessor = bindingPostProcessor;
        this.environment = environment;
    }

    /**
     * 在所有单例初始化后发现、校验并缓存可刷新的配置属性 Bean。
     * Discovers, validates, and caches refreshable configuration-properties Beans after singleton initialization.
     *
     * @throws IllegalStateException 可刷新 Bean 使用不可变绑定或没有可写属性时抛出;
     *                               thrown when a refreshable Bean uses immutable binding or exposes no writable property
     */
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

    /**
     * 重新绑定受本次有效配置变化影响且没有无回退删除项的 Bean。
     * Rebinds Beans affected by effective changes and having no removed property without a fallback.
     *
     * @param changedKeys 发生有效变化的配置键; effectively changed configuration keys
     * @param removedKeys 已删除的配置键; removed configuration keys
     * @return 通过配置属性 Bean 重绑定完成刷新的键; keys refreshed through configuration-properties rebinding
     */
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

    /**
     * 校验候选 Bean 使用可变 JavaBean 绑定并至少公开一个写方法。
     * Validates that a candidate uses mutable JavaBean binding and exposes at least one write method.
     *
     * @param beanName Spring Bean 名称; Spring Bean name
     * @param bean     配置属性 Bean 描述; configuration-properties Bean descriptor
     * @param beanType 用户定义的 Bean 类型; user-defined Bean type
     * @throws IllegalStateException 候选 Bean 无法安全重绑定时抛出; thrown when the candidate cannot be rebound safely
     */
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

    /**
     * 判断指定前缀下是否存在删除后也无法从其他属性源解析的键。
     * Determines whether the prefix contains a removed key that cannot be resolved from another property source.
     *
     * @param prefix      配置属性前缀; configuration-properties prefix
     * @param removedKeys 已删除的配置键; removed configuration keys
     * @return 存在无回退删除键时为 {@code true}; {@code true} when a removed key has no fallback
     */
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

    /**
     * 筛选与配置属性前缀相同或位于该前缀之下的键。
     * Filters keys equal to or nested beneath the configuration-properties prefix.
     *
     * @param prefix 配置属性前缀; configuration-properties prefix
     * @param keys   待筛选的配置键; configuration keys to filter
     * @return 保持输入迭代顺序的匹配键集合; matching keys preserving input iteration order
     */
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

    /**
     * 缓存一个可刷新配置属性 Bean 的绑定信息。
     * Cached binding information for a refreshable configuration-properties Bean.
     *
     * @param name     Spring Bean 名称; Spring Bean name
     * @param instance Bean 实例; Bean instance
     * @param prefix   规范化后的配置属性前缀; normalized configuration-properties prefix
     */
    private record RefreshableBean(
            String name,
            Object instance,
            ConfigurationPropertyName prefix
    ) {
    }
}
