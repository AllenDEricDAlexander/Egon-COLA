package top.egon.cola.component.accessguard.execution;

import top.egon.cola.component.accessguard.core.GuardOutcome;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FallbackMethodCache {

    private final Map<CacheKey, Binding> bindings = new LinkedHashMap<>();

    public synchronized MethodHandle validateAndCache(Executable executable, String fallbackName) {
        if (!(executable instanceof Method method)) {
            throw new IllegalArgumentException("constructors do not support fallback");
        }
        if (fallbackName == null || fallbackName.isBlank()) {
            throw new IllegalArgumentException("fallback method must not be blank");
        }
        CacheKey key = new CacheKey(executable, fallbackName.trim());
        Binding existing = bindings.get(key);
        if (existing != null) {
            return existing.handle();
        }
        Binding binding = resolve(method, fallbackName.trim());
        bindings.put(key, binding);
        return binding.handle();
    }

    public synchronized Optional<MethodHandle> lookup(Executable executable, String fallbackName) {
        if (fallbackName == null) {
            return Optional.empty();
        }
        Binding binding = bindings.get(new CacheKey(executable, fallbackName.trim()));
        return binding == null ? Optional.empty() : Optional.of(binding.handle());
    }

    synchronized Binding binding(Executable executable, String fallbackName) {
        CacheKey key = new CacheKey(executable, fallbackName == null ? "" : fallbackName.trim());
        Binding binding = bindings.get(key);
        if (binding == null) {
            throw new IllegalStateException("fallback was not validated: " + key.fallbackName());
        }
        return binding;
    }

    private static Binding resolve(Method original, String fallbackName) {
        List<Candidate> candidates = new ArrayList<>();
        Class<?> type = original.getDeclaringClass();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method candidate : current.getDeclaredMethods()) {
                if (!candidate.getName().equals(fallbackName)) {
                    continue;
                }
                ArgumentMode mode = argumentMode(original, candidate);
                if (mode != null) {
                    candidates.add(new Candidate(candidate, mode));
                }
            }
        }
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("fallback method not found or has incompatible arguments: " + fallbackName);
        }
        if (candidates.size() > 1) {
            throw new IllegalArgumentException("fallback method is ambiguous: " + fallbackName);
        }
        Candidate candidate = candidates.getFirst();
        if (Modifier.isStatic(original.getModifiers()) && !Modifier.isStatic(candidate.method().getModifiers())) {
            throw new IllegalArgumentException("static methods require a static fallback");
        }
        if (!isReturnCompatible(original.getReturnType(), candidate.method().getReturnType())) {
            throw new IllegalArgumentException("fallback return type is incompatible with original method");
        }
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(
                    candidate.method().getDeclaringClass(), MethodHandles.lookup());
            return new Binding(
                    lookup.unreflect(candidate.method()),
                    candidate.mode(),
                    Modifier.isStatic(candidate.method().getModifiers()));
        } catch (IllegalAccessException exception) {
            throw new IllegalArgumentException("fallback method is not accessible: " + fallbackName, exception);
        }
    }

    private static ArgumentMode argumentMode(Method original, Method fallback) {
        Class<?>[] originalParameters = original.getParameterTypes();
        Class<?>[] fallbackParameters = fallback.getParameterTypes();
        if (Arrays.equals(originalParameters, fallbackParameters)) {
            return ArgumentMode.ARGUMENTS;
        }
        if (fallbackParameters.length == originalParameters.length + 1
                && fallbackParameters[fallbackParameters.length - 1] == GuardOutcome.class
                && Arrays.equals(originalParameters, Arrays.copyOf(fallbackParameters, originalParameters.length))) {
            return ArgumentMode.ARGUMENTS_AND_OUTCOME;
        }
        return fallbackParameters.length == 0 ? ArgumentMode.NONE : null;
    }

    private static boolean isReturnCompatible(Class<?> original, Class<?> fallback) {
        if (original == void.class || fallback == void.class) {
            return original == fallback;
        }
        return wrap(original).isAssignableFrom(wrap(fallback));
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        return switch (type.getName()) {
            case "boolean" -> Boolean.class;
            case "byte" -> Byte.class;
            case "short" -> Short.class;
            case "int" -> Integer.class;
            case "long" -> Long.class;
            case "float" -> Float.class;
            case "double" -> Double.class;
            case "char" -> Character.class;
            default -> Void.class;
        };
    }

    record Binding(MethodHandle handle, ArgumentMode argumentMode, boolean staticMethod) {
    }

    enum ArgumentMode {
        ARGUMENTS,
        ARGUMENTS_AND_OUTCOME,
        NONE
    }

    private record Candidate(Method method, ArgumentMode mode) {
    }

    private record CacheKey(Executable executable, String fallbackName) {
    }
}
