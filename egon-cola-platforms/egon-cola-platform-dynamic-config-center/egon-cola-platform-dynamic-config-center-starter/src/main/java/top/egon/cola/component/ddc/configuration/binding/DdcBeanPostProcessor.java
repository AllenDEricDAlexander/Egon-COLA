package top.egon.cola.component.ddc.configuration.binding;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.util.ClassUtils;
import top.egon.cola.component.ddc.configuration.binding.DdcFieldBindingService;

/**
 * DDC 配置字段绑定的 Spring Bean 后处理器。
 * Spring Bean post-processor for DDC configuration field binding.
 *
 * <p>每个 Spring Bean 初始化完成后，本处理器尽可能解析其用户定义类型，并委托
 * {@link DdcFieldBindingService} 扫描带有 DDC 配置注解的字段。初次字段注入由 Spring 原生
 * {@code @Value} 处理链完成，绑定服务只登记允许运行期刷新的字段。</p>
 *
 * <p>After each Spring Bean is initialized, this processor resolves its user-defined type whenever
 * possible and delegates to {@link DdcFieldBindingService} to scan fields carrying DDC configuration
 * annotations. Initial assignment is handled by Spring's native {@code @Value} pipeline, while the
 * binding service only registers fields that allow runtime refresh.</p>
 *
 * <p>本处理器不会包装或替换 Bean，处理完成后仍返回原 Bean 实例。</p>
 * <p>This processor neither wraps nor replaces a Bean; it returns the original instance.</p>
 */
public class DdcBeanPostProcessor implements BeanPostProcessor, DestructionAwareBeanPostProcessor {

    /**
     * 负责扫描和登记可刷新 DDC 配置字段的服务。 Service that scans and registers refreshable DDC configuration fields.
     */
    private final DdcFieldBindingService fieldBindingService;

    /**
     * 创建 DDC Bean 后处理器。
     * Creates a DDC Bean post-processor.
     *
     * @param fieldBindingService DDC 配置字段绑定服务; DDC configuration field binding service
     */
    public DdcBeanPostProcessor(DdcFieldBindingService fieldBindingService) {
        this.fieldBindingService = fieldBindingService;
    }

    /**
     * 在 Bean 初始化完成后登记其中的 DDC 配置字段。
     * Registers DDC configuration fields after Bean initialization.
     *
     * <p>{@link ClassUtils#getUserClass(Object)} 用于还原 CGLIB 等生成子类背后的用户定义类型，避免只扫描
     * 生成类而遗漏业务类字段。可刷新字段登记失败时，异常会继续向外抛出并终止该 Bean 的创建。</p>
     *
     * <p>{@link ClassUtils#getUserClass(Object)} restores the user-defined type behind generated subclasses
     * such as CGLIB proxies, preventing business fields from being skipped. A refreshable-field registration
     * failure is propagated and aborts creation of the Bean.</p>
     *
     * @param bean     已完成初始化的 Bean 实例; initialized Bean instance
     * @param beanName Bean 在 Spring 容器中的名称; Bean name in the Spring container
     * @return 未被包装或替换的原 Bean 实例; original Bean instance without wrapping or replacement
     * @throws BeansException Spring Bean 后处理失败时抛出; thrown when Spring Bean post-processing fails
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        fieldBindingService.bind(
                beanName,
                bean,
                ClassUtils.getUserClass(bean)
        );
        return bean;
    }

    /**
     * 在 Bean 销毁前移除其动态字段绑定，避免继续持有已销毁的非单例 Bean。
     * Removes dynamic field bindings before bean destruction so destroyed non-singleton beans are not retained.
     *
     * @param bean     即将销毁的 Bean 实例; bean instance about to be destroyed
     * @param beanName Bean 在 Spring 容器中的名称; bean name in the Spring container
     * @throws BeansException Spring Bean 销毁处理失败时抛出; thrown when Spring bean destruction processing fails
     */
    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName)
            throws BeansException {
        fieldBindingService.unbind(bean);
    }
}
