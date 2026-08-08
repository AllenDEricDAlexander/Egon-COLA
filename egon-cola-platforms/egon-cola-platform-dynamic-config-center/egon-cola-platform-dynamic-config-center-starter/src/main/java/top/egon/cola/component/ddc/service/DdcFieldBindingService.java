package top.egon.cola.component.ddc.service;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import top.egon.cola.component.ddc.annotation.DdcValue;
import top.egon.cola.component.ddc.model.vo.DdcFieldBinding;
import top.egon.cola.component.ddc.repository.DdcValueBindingRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 登记并重新解析使用 {@link DdcValue} 声明的可刷新字段。
 * Registers and re-resolves refreshable fields declared with {@link DdcValue}.
 *
 * <p>初次注入完全由 Spring 的 {@code @Value} 处理链完成。本服务只保存可刷新字段，并在动态属性源
 * 替换后通过 {@link ConfigurableListableBeanFactory#resolveDependency} 重新执行同一套占位符、SpEL 和
 * 类型转换语义。</p>
 *
 * <p>Initial injection is handled entirely by Spring's {@code @Value} pipeline. This service only stores
 * refreshable fields and invokes {@link ConfigurableListableBeanFactory#resolveDependency} after the dynamic
 * property source changes, preserving the same placeholder, SpEL, and type-conversion semantics.</p>
 */
public class DdcFieldBindingService {

    /**
     * 保存当前活动字段绑定的注册表。 Registry storing currently active field bindings.
     */
    private final DdcValueBindingRegistry registry;

    /**
     * 使用 Spring 原生依赖解析管线重新计算字段值的 BeanFactory。
     * BeanFactory that re-evaluates field values through Spring's native dependency-resolution pipeline.
     */
    private final ConfigurableListableBeanFactory beanFactory;

    /**
     * 创建字段绑定服务。 Creates the field-binding service.
     *
     * @param registry    字段绑定注册表; field-binding registry
     * @param beanFactory Spring BeanFactory; Spring bean factory
     */
    public DdcFieldBindingService(DdcValueBindingRegistry registry,
                                  ConfigurableListableBeanFactory beanFactory) {
        this.registry = registry;
        this.beanFactory = beanFactory;
    }

    /**
     * 扫描目标类型并登记其中允许动态刷新的 {@link DdcValue} 字段。
     * Scans the target type and registers refreshable {@link DdcValue} fields.
     *
     * @param beanName   Bean 在 Spring 容器中的名称; bean name in the Spring container
     * @param bean       字段所属 Bean; bean owning the fields
     * @param targetClass 待扫描类型，可为代理类型; type to scan, possibly a proxy type
     * @throws IllegalStateException 可刷新字段为静态或 final 字段时抛出; thrown for a static or final refreshable field
     */
    public void bind(String beanName, Object bean, Class<?> targetClass) {
        registry.unregister(bean);
        Class<?> userClass = ClassUtils.getUserClass(targetClass);
        ReflectionUtils.doWithFields(
                userClass,
                field -> register(beanName, bean, field),
                field -> field.isAnnotationPresent(DdcValue.class)
        );
    }

    /**
     * 移除属于指定 Bean 的全部字段绑定。 Removes all field bindings owned by the specified bean.
     *
     * @param bean 待解绑 Bean; bean to unregister
     */
    public void unbind(Object bean) {
        registry.unregister(bean);
    }

    /**
     * 先解析全部字段候选值，再批量写入发生变化的字段。
     * Resolves all candidate values before writing fields whose values changed.
     *
     * @return 包含已写入字段旧值和表达式的刷新结果; refresh result containing previous values and expressions
     * @throws RuntimeException 任一表达式解析、类型转换或字段写入失败时抛出; thrown when resolution, conversion, or writing fails
     */
    public RefreshResult refresh() {
        List<PendingWrite> pendingWrites = new ArrayList<>();
        for (DdcFieldBinding binding : registry.bindings()) {
            Object previousValue = read(binding);
            Object resolvedValue = resolve(binding);
            if (!Objects.equals(previousValue, resolvedValue)) {
                pendingWrites.add(new PendingWrite(
                        binding,
                        previousValue,
                        resolvedValue,
                        expression(binding)
                ));
            }
        }
        List<PendingWrite> appliedWrites = new ArrayList<>();
        try {
            for (PendingWrite pendingWrite : pendingWrites) {
                write(pendingWrite.binding(), pendingWrite.value());
                appliedWrites.add(pendingWrite);
            }
        } catch (RuntimeException exception) {
            rollbackWrites(appliedWrites);
            throw exception;
        }
        return new RefreshResult(appliedWrites);
    }

    /**
     * 使用刷新前保存的字段值回滚一次已完成的字段刷新。
     * Rolls back a completed field refresh using values captured before the refresh.
     *
     * @param result 待回滚刷新结果; refresh result to roll back
     */
    public void rollback(RefreshResult result) {
        rollbackWrites(result.appliedWrites());
    }

    /**
     * 通过 Spring BeanFactory 重新解析字段依赖。
     * Re-resolves a field dependency through the Spring BeanFactory.
     *
     * @param binding 字段绑定; field binding
     * @return 解析并转换后的字段值; resolved and converted field value
     */
    protected Object resolve(DdcFieldBinding binding) {
        DependencyDescriptor descriptor = new DependencyDescriptor(
                binding.getField(),
                true
        );
        descriptor.setContainingClass(
                ClassUtils.getUserClass(binding.getBean())
        );
        return beanFactory.resolveDependency(
                descriptor,
                binding.getBeanName()
        );
    }

    /**
     * 反射读取绑定字段的当前值。 Reads the current bound-field value through reflection.
     *
     * @param binding 字段绑定; field binding
     * @return 当前字段值; current field value
     */
    protected Object read(DdcFieldBinding binding) {
        ReflectionUtils.makeAccessible(binding.getField());
        return ReflectionUtils.getField(
                binding.getField(),
                binding.getBean()
        );
    }

    /**
     * 反射写入绑定字段。 Writes a bound field through reflection.
     *
     * @param binding 字段绑定; field binding
     * @param value   待写入值; value to write
     */
    protected void write(DdcFieldBinding binding, Object value) {
        ReflectionUtils.makeAccessible(binding.getField());
        ReflectionUtils.setField(
                binding.getField(),
                binding.getBean(),
                value
        );
    }

    /**
     * 校验并登记一个可刷新字段。 Validates and registers a refreshable field.
     *
     * @param beanName Bean 名称; bean name
     * @param bean     字段所属 Bean; bean owning the field
     * @param field    已注解字段; annotated field
     */
    private void register(String beanName, Object bean, Field field) {
        DdcValue annotation = field.getAnnotation(DdcValue.class);
        if (!annotation.refreshable()) {
            return;
        }
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
            throw new IllegalStateException(
                    "refreshable @DdcValue field must be an instance, non-final field: "
                            + field.toGenericString()
            );
        }
        registry.register(new DdcFieldBinding(beanName, bean, field));
    }

    /**
     * 返回字段声明的 Spring 配置表达式。 Returns the Spring configuration expression declared on a field.
     *
     * @param binding 字段绑定; field binding
     * @return 配置表达式; configuration expression
     */
    private String expression(DdcFieldBinding binding) {
        return binding.getField().getAnnotation(DdcValue.class).value();
    }

    /**
     * 按逆序尽力恢复已经写入的字段。 Best-effort restores written fields in reverse order.
     *
     * @param appliedWrites 已完成字段写入; completed field writes
     */
    private void rollbackWrites(List<PendingWrite> appliedWrites) {
        for (int index = appliedWrites.size() - 1; index >= 0; index--) {
            PendingWrite appliedWrite = appliedWrites.get(index);
            try {
                write(
                        appliedWrite.binding(),
                        appliedWrite.previousValue()
                );
            } catch (RuntimeException ignored) {
                // Preserve the primary refresh failure while making rollback best effort.
            }
        }
    }

    /**
     * 描述一次成功字段刷新及其回滚数据。 Describes a successful field refresh and its rollback data.
     */
    public static final class RefreshResult {

        /**
         * 已写入字段及其旧值。 Applied fields and their previous values.
         */
        private final List<PendingWrite> appliedWrites;

        /**
         * 创建不可变刷新结果。 Creates an immutable refresh result.
         *
         * @param appliedWrites 已完成字段写入; completed field writes
         */
        private RefreshResult(List<PendingWrite> appliedWrites) {
            this.appliedWrites = List.copyOf(appliedWrites);
        }

        /**
         * 返回本次实际变化的配置表达式。 Returns configuration expressions whose field values changed.
         *
         * @return 保持登记顺序的表达式集合; expressions preserving registration order
         */
        public Set<String> refreshedExpressions() {
            Set<String> expressions = new LinkedHashSet<>();
            appliedWrites.forEach(write -> expressions.add(
                    write.expression()
            ));
            return Collections.unmodifiableSet(expressions);
        }

        /**
         * 返回已写入字段及旧值。 Returns applied fields and previous values.
         *
         * @return 已完成字段写入; completed field writes
         */
        private List<PendingWrite> appliedWrites() {
            return appliedWrites;
        }
    }

    /**
     * 保存一次延迟字段写入及其回滚值。
     * Stores a deferred field write and its rollback value.
     *
     * @param binding       字段绑定; field binding
     * @param previousValue 写入前字段值; field value before writing
     * @param value         待写入的新值; value to write
     * @param expression    Spring 配置表达式; Spring configuration expression
     */
    private record PendingWrite(
            DdcFieldBinding binding,
            Object previousValue,
            Object value,
            String expression
    ) {
    }
}
