package top.egon.cola.component.bytecode.starter.methodextension;

import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;
import top.egon.cola.component.bytecode.bridge.MethodMetadata;

import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class MethodMetadataResolver {

    private static final ClassValue<ConcurrentMap<Long, Method>> METHODS =
            new ClassValue<>() {
                @Override
                protected ConcurrentMap<Long, Method> computeValue(Class<?> type) {
                    return new ConcurrentHashMap<>();
                }
            };

    private static final ClassValue<ConcurrentMap<Long, Constructor<?>>> CONSTRUCTORS =
            new ClassValue<>() {
                @Override
                protected ConcurrentMap<Long, Constructor<?>> computeValue(Class<?> type) {
                    return new ConcurrentHashMap<>();
                }
            };

    public Method resolve(Class<?> declaringClass, long methodId) {
        return resolve(declaringClass, methodId, BridgeCapability.METHOD_EXTENSION);
    }

    public Method resolve(
            Class<?> declaringClass,
            long methodId,
            BridgeCapability requiredCapability
    ) {
        return METHODS.get(declaringClass).computeIfAbsent(
                methodId, ignored -> resolveUncached(declaringClass, methodId, requiredCapability));
    }

    public Constructor<?> resolveConstructor(Class<?> declaringClass, long methodId) {
        return CONSTRUCTORS.get(declaringClass).computeIfAbsent(
                methodId, ignored -> resolveConstructorUncached(declaringClass, methodId));
    }

    private Constructor<?> resolveConstructorUncached(Class<?> declaringClass, long methodId) {
        MethodMetadata metadata = DispatcherRegistry.method(
                        declaringClass.getClassLoader(), methodId)
                .orElseThrow(() -> new IllegalStateException(
                        "No transformed constructor metadata for ID " + methodId));
        String owner = declaringClass.getName().replace('.', '/');
        if (!owner.equals(metadata.owner()) || !metadata.constructor()
                || !metadata.features().contains(BridgeCapability.ACCESS_GUARD)) {
            throw new IllegalStateException(
                    "Access Guard constructor metadata does not match "
                            + declaringClass.getName());
        }
        MethodType methodType = MethodType.fromMethodDescriptorString(
                metadata.methodDescriptor(), declaringClass.getClassLoader());
        try {
            Constructor<?> constructor = declaringClass.getDeclaredConstructor(
                    methodType.parameterArray());
            if (!constructor.trySetAccessible()) {
                throw new IllegalStateException(
                        "Cannot access transformed constructor " + constructor.toGenericString());
            }
            return constructor;
        } catch (NoSuchMethodException failure) {
            throw new IllegalStateException(
                    "Cannot resolve transformed constructor " + metadata.owner()
                            + metadata.methodDescriptor(), failure);
        }
    }

    private Method resolveUncached(
            Class<?> declaringClass,
            long methodId,
            BridgeCapability requiredCapability
    ) {
        MethodMetadata metadata = DispatcherRegistry.method(
                        declaringClass.getClassLoader(), methodId)
                .orElseThrow(() -> new IllegalStateException(
                        "No transformed method metadata for ID " + methodId));
        String owner = declaringClass.getName().replace('.', '/');
        if (!owner.equals(metadata.owner())
                || !metadata.features().contains(requiredCapability)) {
            throw new IllegalStateException(
                    requiredCapability + " metadata does not match " + declaringClass.getName());
        }
        MethodType methodType = MethodType.fromMethodDescriptorString(
                metadata.methodDescriptor(), declaringClass.getClassLoader());
        Method method = find(
                declaringClass,
                metadata.methodName(),
                methodType,
                new HashSet<>()
        );
        if (method == null) {
            throw new IllegalStateException(
                    "Cannot resolve transformed method " + metadata.owner() + "#"
                            + metadata.methodName() + metadata.methodDescriptor());
        }
        if (!method.trySetAccessible()) {
            throw new IllegalStateException(
                    "Cannot access transformed method " + method.toGenericString());
        }
        return method;
    }

    private Method find(
            Class<?> type,
            String methodName,
            MethodType methodType,
            Set<Class<?>> visited
    ) {
        if (type == null || !visited.add(type)) {
            return null;
        }
        try {
            Method method = type.getDeclaredMethod(methodName, methodType.parameterArray());
            if (method.getReturnType() == methodType.returnType()) {
                return method;
            }
        } catch (NoSuchMethodException ignored) {
        }
        for (Class<?> interfaceType : type.getInterfaces()) {
            Method method = find(interfaceType, methodName, methodType, visited);
            if (method != null) {
                return method;
            }
        }
        return find(type.getSuperclass(), methodName, methodType, visited);
    }
}
