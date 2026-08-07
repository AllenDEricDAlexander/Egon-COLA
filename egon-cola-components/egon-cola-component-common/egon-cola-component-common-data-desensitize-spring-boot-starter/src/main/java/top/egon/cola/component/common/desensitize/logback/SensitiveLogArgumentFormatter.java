package top.egon.cola.component.common.desensitize.logback;

import top.egon.cola.component.common.desensitize.annotation.Sensitive;
import top.egon.cola.component.common.desensitize.annotation.SensitiveScene;
import top.egon.cola.component.common.desensitize.metadata.SensitiveMetadataResolver;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategyRegistry;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

final class SensitiveLogArgumentFormatter {

    private static final Object INACCESSIBLE = new Object();

    private static final String INACCESSIBLE_TEXT = "<inaccessible>";

    private static final String CIRCULAR = "<circular>";

    private final SensitiveMetadataResolver metadataResolver;

    private final Map<Class<?>, List<LogProperty>> propertyCache =
            new ConcurrentHashMap<>();

    SensitiveLogArgumentFormatter(SensitiveMetadataResolver metadataResolver) {
        this.metadataResolver = metadataResolver;
    }

    Object[] sanitize(Object[] arguments,
                      SensitiveStrategyRegistry strategyRegistry) {
        Object[] sanitized = arguments.clone();
        SanitizationContext context = new SanitizationContext(
                strategyRegistry,
                new IdentityHashMap<>()
        );
        for (int index = 0; index < sanitized.length; index++) {
            sanitized[index] = sanitizeValue(sanitized[index], context).value();
        }
        return sanitized;
    }

    private SanitizedValue sanitizeValue(Object value,
                                         SanitizationContext context) {
        if (value == null || isSimpleValue(value)) {
            return new SanitizedValue(value, false);
        }
        if (context.active().put(value, Boolean.TRUE) != null) {
            return new SanitizedValue(CIRCULAR, false);
        }
        try {
            if (value.getClass().isArray()) {
                return sanitizeArray(value, context);
            }
            if (value instanceof Collection<?> collection) {
                return sanitizeCollection(collection, context);
            }
            if (value instanceof Map<?, ?> map) {
                return sanitizeMap(map, context);
            }
            if (isJdkValue(value)) {
                return new SanitizedValue(value, false);
            }
            return sanitizeBean(value, context);
        } finally {
            context.active().remove(value);
        }
    }

    private SanitizedValue sanitizeArray(Object array,
                                         SanitizationContext context) {
        int length = Array.getLength(array);
        List<Object> values = new ArrayList<>(length);
        boolean changed = false;
        for (int index = 0; index < length; index++) {
            SanitizedValue sanitized = sanitizeValue(Array.get(array, index), context);
            values.add(sanitized.value());
            changed |= sanitized.changed();
        }
        return changed
                ? new SanitizedValue(renderCollection(values, "[", "]"), true)
                : new SanitizedValue(array, false);
    }

    private SanitizedValue sanitizeCollection(Collection<?> collection,
                                              SanitizationContext context) {
        List<Object> values = new ArrayList<>(collection.size());
        boolean changed = false;
        for (Object item : collection) {
            SanitizedValue sanitized = sanitizeValue(item, context);
            values.add(sanitized.value());
            changed |= sanitized.changed();
        }
        return changed
                ? new SanitizedValue(renderCollection(values, "[", "]"), true)
                : new SanitizedValue(collection, false);
    }

    private SanitizedValue sanitizeMap(Map<?, ?> map,
                                       SanitizationContext context) {
        List<String> entries = new ArrayList<>(map.size());
        boolean changed = false;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            SanitizedValue key = sanitizeValue(entry.getKey(), context);
            SanitizedValue value = sanitizeValue(entry.getValue(), context);
            entries.add(render(key.value()) + "=" + render(value.value()));
            changed |= key.changed() || value.changed();
        }
        return changed
                ? new SanitizedValue("{" + String.join(", ", entries) + "}", true)
                : new SanitizedValue(map, false);
    }

    private SanitizedValue sanitizeBean(Object bean,
                                        SanitizationContext context) {
        List<LogProperty> properties = propertyCache.computeIfAbsent(
                bean.getClass(),
                this::resolveProperties
        );
        List<String> values = new ArrayList<>(properties.size());
        boolean changed = false;
        for (LogProperty property : properties) {
            Object value = property.read(bean);
            Sensitive sensitive = metadataResolver.resolve(
                            property.sensitive(),
                            SensitiveScene.LOG
                    )
                    .orElse(null);
            if (sensitive != null && value != null && value != INACCESSIBLE) {
                value = context.strategyRegistry()
                        .mask(sensitive.type(), String.valueOf(value));
                changed = true;
            } else if (value != null && value != INACCESSIBLE) {
                SanitizedValue nested = sanitizeValue(value, context);
                value = nested.value();
                changed |= nested.changed();
            }
            values.add(property.name() + "=" + render(value));
        }
        if (!changed) {
            return new SanitizedValue(bean, false);
        }
        return new SanitizedValue(bean.getClass().getSimpleName()
                + "{" + String.join(", ", values) + "}", true);
    }

    private List<LogProperty> resolveProperties(Class<?> beanType) {
        LinkedHashMap<String, LogProperty> properties = new LinkedHashMap<>();
        Deque<Class<?>> hierarchy = new ArrayDeque<>();
        for (Class<?> current = beanType;
             current != null && current != Object.class;
             current = current.getSuperclass()) {
            hierarchy.push(current);
        }
        while (!hierarchy.isEmpty()) {
            Class<?> current = hierarchy.pop();
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                makeAccessible(field);
                properties.put(
                        field.getName(),
                        LogProperty.forField(field, field.getAnnotation(Sensitive.class))
                );
            }
        }
        hierarchy.clear();
        for (Class<?> current = beanType;
             current != null && current != Object.class;
             current = current.getSuperclass()) {
            hierarchy.push(current);
        }
        while (!hierarchy.isEmpty()) {
            Class<?> current = hierarchy.pop();
            for (Method method : current.getDeclaredMethods()) {
                Sensitive sensitive = method.getAnnotation(Sensitive.class);
                if (sensitive == null
                        || Modifier.isStatic(method.getModifiers())
                        || method.getParameterCount() != 0) {
                    continue;
                }
                makeAccessible(method);
                properties.put(
                        propertyName(method),
                        LogProperty.forMethod(propertyName(method), method, sensitive)
                );
            }
        }
        return List.copyOf(properties.values());
    }

    private void makeAccessible(AccessibleObject member) {
        try {
            member.trySetAccessible();
        } catch (RuntimeException ignored) {
            // read() emits an inaccessible marker instead of falling back to raw toString().
        }
    }

    private String propertyName(Method method) {
        String name = method.getName();
        if (name.startsWith("get") && name.length() > 3) {
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        }
        if (name.startsWith("is") && name.length() > 2) {
            return Character.toLowerCase(name.charAt(2)) + name.substring(3);
        }
        return name;
    }

    private String renderCollection(List<?> values, String prefix, String suffix) {
        return values.stream()
                .map(this::render)
                .collect(Collectors.joining(", ", prefix, suffix));
    }

    private String render(Object value) {
        if (value == null) {
            return "null";
        }
        return value == INACCESSIBLE ? INACCESSIBLE_TEXT : String.valueOf(value);
    }

    private boolean isSimpleValue(Object value) {
        return value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Enum<?>
                || value instanceof Class<?>
                || value instanceof Throwable;
    }

    private boolean isJdkValue(Object value) {
        return value.getClass().getPackageName().startsWith("java.");
    }

    private record SanitizedValue(Object value, boolean changed) {
    }

    private record SanitizationContext(
            SensitiveStrategyRegistry strategyRegistry,
            IdentityHashMap<Object, Boolean> active) {
    }

    private record LogProperty(String name,
                               Field field,
                               Method method,
                               Sensitive sensitive) {

        static LogProperty forField(Field field, Sensitive sensitive) {
            return new LogProperty(field.getName(), field, null, sensitive);
        }

        static LogProperty forMethod(String name, Method method, Sensitive sensitive) {
            return new LogProperty(name, null, method, sensitive);
        }

        Object read(Object bean) {
            try {
                return field != null ? field.get(bean) : method.invoke(bean);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                return INACCESSIBLE;
            }
        }
    }
}
