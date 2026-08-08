package top.egon.cola.component.ddc.service;

import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import top.egon.cola.component.ddc.annotation.DdcValue;
import top.egon.cola.component.ddc.common.DdcException;
import top.egon.cola.component.ddc.common.DdcValueConverter;
import top.egon.cola.component.ddc.common.DdcValueDefinition;
import top.egon.cola.component.ddc.common.DdcValueParser;
import top.egon.cola.component.ddc.model.vo.DdcFieldBinding;
import top.egon.cola.component.ddc.repository.DdcLocalConfigRepository;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 扫描、保存并更新使用 {@link DdcValue} 声明的字段绑定。
 * Scans, stores, and updates field bindings declared with {@link DdcValue}.
 *
 * <p>一次配置更新会先完成全部类型转换，再执行字段写入；写入失败时按逆序尽力恢复已经修改的字段。</p>
 * <p>A configuration update converts all values before writing fields and best-effort restores modified fields in reverse order on failure.</p>
 */
public class DdcFieldBindingService {

    /**
     * 保存按配置键索引的字段绑定。 Repository storing field bindings indexed by configuration key.
     */
    private final DdcLocalConfigRepository repository;

    /**
     * 将字符串配置转换为字段目标类型。 Converts string configuration into field target types.
     */
    private final DdcValueConverter converter;

    /**
     * 解析初始配置值的 Spring 环境。 Spring environment resolving initial configuration values.
     */
    private final Environment environment;

    /**
     * 使用独立标准环境创建字段绑定服务。
     * Creates the field binding service with a standalone standard environment.
     *
     * @param repository 本地配置仓库; local configuration repository
     * @param converter  配置值转换器; configuration value converter
     */
    public DdcFieldBindingService(DdcLocalConfigRepository repository, DdcValueConverter converter) {
        this(repository, converter, new StandardEnvironment());
    }

    /**
     * 使用指定 Spring 环境创建字段绑定服务。
     * Creates the field binding service with the specified Spring environment.
     *
     * @param repository  本地配置仓库; local configuration repository
     * @param converter   配置值转换器; configuration value converter
     * @param environment 用于初始值解析的环境; environment used for initial value resolution
     */
    public DdcFieldBindingService(DdcLocalConfigRepository repository,
                                  DdcValueConverter converter,
                                  Environment environment) {
        this.repository = repository;
        this.converter = converter;
        this.environment = environment;
    }

    /**
     * 扫描目标类型中带 {@link DdcValue} 的字段并为 Bean 建立绑定。
     * Scans fields annotated with {@link DdcValue} on the target type and binds them to the Bean.
     *
     * @param bean        字段所属 Bean; Bean owning the fields
     * @param targetClass 待扫描类型，可为代理类型; type to scan, possibly a proxy type
     */
    public void bind(Object bean, Class<?> targetClass) {
        Class<?> userClass = ClassUtils.getUserClass(targetClass);
        ReflectionUtils.doWithFields(userClass, field -> bindField(bean, field), field -> field.isAnnotationPresent(DdcValue.class));
    }

    /**
     * 原子式更新指定键的所有可刷新字段绑定。
     * Atomically updates all refreshable field bindings for the specified key.
     *
     * @param key     配置键; configuration key
     * @param value   新配置值，删除时为 {@code null}; new configuration value, or {@code null} on removal
     * @param version 配置版本，仅作为调用契约携带; configuration version carried by the call contract
     * @throws DdcException     必填值缺失时抛出; thrown when a required value is missing
     * @throws RuntimeException 转换或字段写入失败时抛出; thrown when conversion or field writing fails
     */
    public void apply(String key, String value, long version) {
        List<DdcFieldBinding> bindings = repository.bindings(key);
        List<PendingWrite> pendingWrites = new ArrayList<>();
        for (DdcFieldBinding binding : bindings) {
            if (binding.isRefreshable()) {
                String resolvedValue = value == null
                        ? binding.getDefaultValue()
                        : value;
                if ((resolvedValue == null || resolvedValue.isEmpty())
                        && binding.isRequired()) {
                    throw new DdcException(
                            "required DDC property is missing: " + key
                    );
                }
                Object converted = converter.convert(
                        resolvedValue,
                        binding.getTargetType()
                );
                pendingWrites.add(new PendingWrite(binding, read(binding), converted));
            }
        }
        List<PendingWrite> appliedWrites = new ArrayList<>();
        try {
            for (PendingWrite pendingWrite : pendingWrites) {
                write(pendingWrite.binding(), pendingWrite.value());
                appliedWrites.add(pendingWrite);
            }
        } catch (RuntimeException exception) {
            for (int index = appliedWrites.size() - 1; index >= 0; index--) {
                PendingWrite appliedWrite = appliedWrites.get(index);
                try {
                    write(appliedWrite.binding(), appliedWrite.previousValue());
                } catch (RuntimeException ignored) {
                    // Best-effort rollback keeps the original write failure as the primary exception.
                }
            }
            throw exception;
        }
    }

    /**
     * 判断配置键是否具有至少一个可刷新字段绑定。
     * Indicates whether a key has at least one refreshable field binding.
     *
     * @param key 配置键; configuration key
     * @return 存在可刷新绑定时为 {@code true}; {@code true} when a refreshable binding exists
     */
    public boolean hasRefreshableBinding(String key) {
        return repository.bindings(key).stream()
                .anyMatch(DdcFieldBinding::isRefreshable);
    }

    /**
     * 反射读取绑定字段的当前值。
     * Reads the current bound-field value through reflection.
     *
     * @param binding 字段绑定; field binding
     * @return 当前字段值; current field value
     */
    protected Object read(DdcFieldBinding binding) {
        ReflectionUtils.makeAccessible(binding.getField());
        return ReflectionUtils.getField(binding.getField(), binding.getBean());
    }

    /**
     * 反射写入绑定字段。
     * Writes a bound field through reflection.
     *
     * @param binding 字段绑定; field binding
     * @param value   待写入值; value to write
     */
    protected void write(DdcFieldBinding binding, Object value) {
        ReflectionUtils.makeAccessible(binding.getField());
        ReflectionUtils.setField(binding.getField(), binding.getBean(), value);
    }

    /**
     * 解析字段注解、登记绑定并应用初始值。
     * Parses the field annotation, registers the binding, and applies its initial value.
     *
     * @param bean  字段所属 Bean; Bean owning the field
     * @param field 已注解字段; annotated field
     */
    private void bindField(Object bean, Field field) {
        DdcValue annotation = field.getAnnotation(DdcValue.class);
        Class<?> targetType = annotation.type() == Object.class ? field.getType() : annotation.type();
        DdcValueDefinition definition = DdcValueParser.parse(annotation.value(), annotation.key(), annotation.defaultValue(), targetType);
        DdcFieldBinding binding = new DdcFieldBinding(bean, field, definition.getKey(), definition.getDefaultValue(),
                definition.getType(), annotation.required(), annotation.refreshable());
        repository.addBinding(definition.getKey(), binding);
        applyInitialValue(binding);
    }

    /**
     * 从环境值或注解默认值初始化一个字段绑定。
     * Initializes a field binding from the environment value or annotation default.
     *
     * @param binding 待初始化的字段绑定; field binding to initialize
     * @throws DdcException 必填值缺失或无法转换时抛出; thrown when a required value is missing or cannot be converted
     */
    private void applyInitialValue(DdcFieldBinding binding) {
        String value = environment.getProperty(binding.getConfigKey());
        if (value == null || value.isEmpty()) {
            value = binding.getDefaultValue();
        }
        if (value == null || value.isEmpty()) {
            if (binding.isRequired()) {
                throw new DdcException(
                        "required DDC property is missing: "
                                + binding.getConfigKey()
                );
            }
            return;
        }
        try {
            Object converted = converter.convert(value, binding.getTargetType());
            write(binding, converted);
        } catch (Exception e) {
            if (binding.isRequired()) {
                throw new DdcException("apply default config value failed", e);
            }
        }
    }

    /**
     * 保存一次延迟字段写入及其回滚值。
     * Stores a deferred field write and its rollback value.
     *
     * @param binding       字段绑定; field binding
     * @param previousValue 写入前字段值; field value before writing
     * @param value         待写入的新值; new value to write
     */
    private record PendingWrite(
            DdcFieldBinding binding,
            Object previousValue,
            Object value
    ) {
    }
}
