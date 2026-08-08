package top.egon.cola.component.ddc.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ClassUtils;
import top.egon.cola.component.ddc.service.DdcFieldBindingService;

/**
 * DDC 配置字段绑定的 Spring Bean 后处理器。
 * Spring Bean post-processor for DDC configuration field binding.
 *
 * <p>每个 Spring Bean 初始化完成后，本处理器尽可能解析其用户定义类型，并委托
 * {@link DdcFieldBindingService} 扫描带有 DDC 配置注解的字段。绑定服务负责保存字段绑定关系，
 * 并在声明了默认值时完成类型转换和初始赋值。</p>
 *
 * <p>After each Spring Bean is initialized, this processor resolves its user-defined type whenever
 * possible and delegates to {@link DdcFieldBindingService} to scan fields carrying DDC configuration
 * annotations. The binding service stores field bindings and performs type conversion and initial
 * assignment when a default value is declared.</p>
 *
 * <p>本处理器不会包装或替换 Bean，处理完成后仍返回原 Bean 实例。</p>
 * <p>This processor neither wraps nor replaces a Bean; it returns the original instance.</p>
 */
public class DdcBeanPostProcessor implements BeanPostProcessor {

    /**
     * 负责扫描、登记并初始化 DDC 配置字段的服务。 Service that scans, registers, and initializes DDC configuration fields.
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
     * 生成类而遗漏业务类字段。字段绑定或必填默认值初始化失败时，异常会继续向外抛出并终止该 Bean 的创建。</p>
     *
     * <p>{@link ClassUtils#getUserClass(Object)} restores the user-defined type behind generated subclasses
     * such as CGLIB proxies, preventing business fields from being skipped. A field binding or required-default
     * initialization failure is propagated and aborts creation of the Bean.</p>
     *
     * @param bean     已完成初始化的 Bean 实例; initialized Bean instance
     * @param beanName Bean 在 Spring 容器中的名称; Bean name in the Spring container
     * @return 未被包装或替换的原 Bean 实例; original Bean instance without wrapping or replacement
     * @throws BeansException Spring Bean 后处理失败时抛出; thrown when Spring Bean post-processing fails
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        fieldBindingService.bind(bean, ClassUtils.getUserClass(bean));
        return bean;
    }
}
