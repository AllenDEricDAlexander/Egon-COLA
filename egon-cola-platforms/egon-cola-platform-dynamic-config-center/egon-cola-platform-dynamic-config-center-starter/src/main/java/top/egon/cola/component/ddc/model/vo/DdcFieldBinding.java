package top.egon.cola.component.ddc.model.vo;

import java.lang.reflect.Field;

/**
 * 描述一个需要由 DDC 重新解析 Spring 配置表达式的字段。
 * Describes a field whose Spring configuration expression must be re-resolved by DDC.
 */
public class DdcFieldBinding {

    /**
     * 目标 Bean 在 Spring 容器中的名称。 Name of the target bean in the Spring container.
     */
    private final String beanName;

    /**
     * 持有目标字段的 Bean 实例。 Bean instance that owns the target field.
     */
    private final Object bean;

    /**
     * 接收配置值的反射字段。 Reflective field that receives the configuration value.
     */
    private final Field field;

    /**
     * 创建字段绑定描述。 Creates a field-binding descriptor.
     *
     * @param beanName 目标 Bean 名称; target bean name
     * @param bean     持有字段的 Bean 实例; bean instance that owns the field
     * @param field    接收配置值的字段; field that receives the configuration value
     */
    public DdcFieldBinding(String beanName, Object bean, Field field) {
        this.beanName = beanName;
        this.bean = bean;
        this.field = field;
    }

    /**
     * 返回目标 Bean 名称。 Returns the target bean name.
     *
     * @return Bean 名称; bean name
     */
    public String getBeanName() {
        return beanName;
    }

    /**
     * 返回持有目标字段的 Bean。 Returns the bean that owns the target field.
     *
     * @return Bean 实例; bean instance
     */
    public Object getBean() {
        return bean;
    }

    /**
     * 返回接收配置值的字段。 Returns the field that receives the configuration value.
     *
     * @return 反射字段; reflective field
     */
    public Field getField() {
        return field;
    }
}
