package top.egon.cola.component.ddc.service;

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
import java.util.List;

public class DdcFieldBindingService {

    private final DdcLocalConfigRepository repository;

    private final DdcValueConverter converter;

    public DdcFieldBindingService(DdcLocalConfigRepository repository, DdcValueConverter converter) {
        this.repository = repository;
        this.converter = converter;
    }

    public void bind(Object bean, Class<?> targetClass) {
        Class<?> userClass = ClassUtils.getUserClass(targetClass);
        ReflectionUtils.doWithFields(userClass, field -> bindField(bean, field), field -> field.isAnnotationPresent(DdcValue.class));
    }

    public void apply(String key, String value, long version) {
        List<DdcFieldBinding> bindings = repository.bindings(key);
        for (DdcFieldBinding binding : bindings) {
            if (binding.isRefreshable()) {
                Object converted = converter.convert(value, binding.getTargetType());
                ReflectionUtils.makeAccessible(binding.getField());
                ReflectionUtils.setField(binding.getField(), binding.getBean(), converted);
            }
        }
        repository.updateVersion(key, version);
    }

    private void bindField(Object bean, Field field) {
        DdcValue annotation = field.getAnnotation(DdcValue.class);
        Class<?> targetType = annotation.type() == Object.class ? field.getType() : annotation.type();
        DdcValueDefinition definition = DdcValueParser.parse(annotation.value(), annotation.key(), annotation.defaultValue(), targetType);
        DdcFieldBinding binding = new DdcFieldBinding(bean, field, definition.getKey(), definition.getDefaultValue(),
                definition.getType(), annotation.required(), annotation.refreshable());
        repository.addBinding(definition.getKey(), binding);
        applyDefaultValue(binding);
    }

    private void applyDefaultValue(DdcFieldBinding binding) {
        if (binding.getDefaultValue() == null || binding.getDefaultValue().isEmpty()) {
            return;
        }
        try {
            Object converted = converter.convert(binding.getDefaultValue(), binding.getTargetType());
            ReflectionUtils.makeAccessible(binding.getField());
            ReflectionUtils.setField(binding.getField(), binding.getBean(), converted);
            repository.updateVersion(binding.getConfigKey(), 0L);
        } catch (Exception e) {
            if (binding.isRequired()) {
                throw new DdcException("apply default config value failed", e);
            }
        }
    }
}
