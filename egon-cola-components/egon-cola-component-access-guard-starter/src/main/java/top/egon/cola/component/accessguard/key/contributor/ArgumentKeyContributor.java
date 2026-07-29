package top.egon.cola.component.accessguard.key.contributor;

import top.egon.cola.component.accessguard.api.GuardKey;
import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.plan.KeyConfig;
import top.egon.cola.component.accessguard.key.GuardKeyPart;
import top.egon.cola.component.accessguard.key.GuardKeyResolutionException;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;

public final class ArgumentKeyContributor implements GuardKeyContributor {

    @Override
    public String id() {
        return "ARGUMENT";
    }

    @Override
    public List<GuardKeyPart> contribute(GuardInvocation invocation, KeyConfig config) {
        if (invocation.executable() == null) {
            return List.of();
        }
        Parameter[] parameters = invocation.executable().getParameters();
        Object[] arguments = invocation.arguments();
        List<GuardKeyPart> parts = new ArrayList<>();
        for (int index = 0; index < parameters.length && index < arguments.length; index++) {
            Parameter parameter = parameters[index];
            Object argument = arguments[index];
            GuardKey annotation = parameter.getAnnotation(GuardKey.class);
            if (annotation != null) {
                add(parts, annotation, name(annotation.value(), parameter.getName()), argument);
            } else if (argument != null) {
                addAnnotatedMembers(parts, argument);
            }
        }
        return List.copyOf(parts);
    }

    private static void addAnnotatedMembers(List<GuardKeyPart> parts, Object argument) {
        Class<?> type = argument.getClass();
        if (type.isRecord()) {
            for (RecordComponent component : type.getRecordComponents()) {
                GuardKey annotation = component.getAnnotation(GuardKey.class);
                if (annotation == null) {
                    continue;
                }
                try {
                    add(parts, annotation, name(annotation.value(), component.getName()), component.getAccessor().invoke(argument));
                } catch (ReflectiveOperationException exception) {
                    throw new GuardKeyResolutionException("RECORD_COMPONENT_UNREADABLE");
                }
            }
            return;
        }
        for (Field field : type.getDeclaredFields()) {
            GuardKey annotation = field.getAnnotation(GuardKey.class);
            if (annotation == null) {
                continue;
            }
            try {
                if (!field.trySetAccessible()) {
                    throw new GuardKeyResolutionException("FIELD_UNREADABLE");
                }
                add(parts, annotation, name(annotation.value(), field.getName()), field.get(argument));
            } catch (IllegalAccessException exception) {
                throw new GuardKeyResolutionException("FIELD_UNREADABLE");
            }
        }
    }

    private static void add(List<GuardKeyPart> parts, GuardKey annotation, String name, Object value) {
        if (value == null) {
            if (annotation.required()) {
                throw new GuardKeyResolutionException("REQUIRED_PART_MISSING");
            }
            return;
        }
        parts.add(new GuardKeyPart(name, String.valueOf(value), annotation.order()));
    }

    private static String name(String configured, String fallback) {
        return configured == null || configured.isBlank() ? fallback : configured.trim();
    }
}
