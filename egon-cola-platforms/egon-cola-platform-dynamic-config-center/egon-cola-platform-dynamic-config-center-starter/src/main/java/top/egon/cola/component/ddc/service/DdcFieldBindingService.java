package top.egon.cola.component.ddc.service;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
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

public class DdcFieldBindingService {

    private final DdcLocalConfigRepository repository;

    private final DdcValueConverter converter;

    private final Environment environment;

    public DdcFieldBindingService(DdcLocalConfigRepository repository, DdcValueConverter converter) {
        this(repository, converter, new StandardEnvironment());
    }

    public DdcFieldBindingService(DdcLocalConfigRepository repository,
                                  DdcValueConverter converter,
                                  Environment environment) {
        this.repository = repository;
        this.converter = converter;
        this.environment = environment;
    }

    public void bind(Object bean, Class<?> targetClass) {
        Class<?> userClass = ClassUtils.getUserClass(targetClass);
        ReflectionUtils.doWithFields(userClass, field -> bindField(bean, field), field -> field.isAnnotationPresent(DdcValue.class));
    }

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

    public boolean hasRefreshableBinding(String key) {
        return repository.bindings(key).stream()
                .anyMatch(DdcFieldBinding::isRefreshable);
    }

    protected Object read(DdcFieldBinding binding) {
        ReflectionUtils.makeAccessible(binding.getField());
        return ReflectionUtils.getField(binding.getField(), binding.getBean());
    }

    protected void write(DdcFieldBinding binding, Object value) {
        ReflectionUtils.makeAccessible(binding.getField());
        ReflectionUtils.setField(binding.getField(), binding.getBean(), value);
    }

    private void bindField(Object bean, Field field) {
        DdcValue annotation = field.getAnnotation(DdcValue.class);
        Class<?> targetType = annotation.type() == Object.class ? field.getType() : annotation.type();
        DdcValueDefinition definition = DdcValueParser.parse(annotation.value(), annotation.key(), annotation.defaultValue(), targetType);
        DdcFieldBinding binding = new DdcFieldBinding(bean, field, definition.getKey(), definition.getDefaultValue(),
                definition.getType(), annotation.required(), annotation.refreshable());
        repository.addBinding(definition.getKey(), binding);
        applyInitialValue(binding);
    }

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

    private record PendingWrite(
            DdcFieldBinding binding,
            Object previousValue,
            Object value
    ) {
    }
}
