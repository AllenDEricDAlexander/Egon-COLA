package top.egon.cola.component.methodextension.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import top.egon.cola.component.methodextension.exception.MethodExtensionConfigurationException;
import top.egon.cola.component.methodextension.exception.MethodExtensionResponseException;
import top.egon.cola.component.methodextension.handler.MethodExtensionDecision;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.List;

public class MethodExtensionResponseResolver {

    private final ObjectProvider<ObjectMapper> objectMappers;

    public MethodExtensionResponseResolver(ObjectProvider<ObjectMapper> objectMappers) {
        this.objectMappers = objectMappers;
    }

    public Object resolve(Method method, MethodExtensionDecision decision, String returnJson) {
        if (decision.allowed()) {
            throw new MethodExtensionConfigurationException(
                    "Cannot resolve a rejection response from an allow decision for " + method.toGenericString()
            );
        }
        if (decision.responseProvided()) {
            return validateDirectResponse(method, decision.response());
        }
        if (method.getReturnType() == Void.TYPE || method.getReturnType() == Void.class) {
            if (StringUtils.hasText(returnJson)) {
                throw new MethodExtensionConfigurationException(
                        "returnJson must be empty for void method " + method.toGenericString()
                );
            }
            return null;
        }
        if (!StringUtils.hasText(returnJson)) {
            throw new MethodExtensionConfigurationException(
                    "Method extension rejected without a response or returnJson for " + method.toGenericString()
            );
        }
        if (method.getReturnType() == String.class) {
            return returnJson;
        }
        Type genericReturnType = method.getGenericReturnType();
        if (containsTypeVariable(genericReturnType)) {
            throw new MethodExtensionResponseException(
                    "Cannot convert returnJson for unresolved type variable on " + method.toGenericString()
            );
        }
        ObjectMapper objectMapper = requireUniqueObjectMapper(method);
        try {
            JavaType javaType = objectMapper.getTypeFactory().constructType(genericReturnType);
            return objectMapper.readerFor(javaType).readValue(returnJson);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new MethodExtensionResponseException(
                    "Failed to convert returnJson for " + method.toGenericString(),
                    exception
            );
        }
    }

    private Object validateDirectResponse(Method method, Object response) {
        if (!ClassUtils.isAssignableValue(method.getReturnType(), response)) {
            throw new MethodExtensionResponseException(
                    "Method extension response type " + response.getClass().getName()
                            + " is not assignable to " + method.getReturnType().getName()
                            + " for " + method.toGenericString()
            );
        }
        return response;
    }

    private ObjectMapper requireUniqueObjectMapper(Method method) {
        List<ObjectMapper> candidates = objectMappers.orderedStream().toList();
        if (candidates.isEmpty()) {
            throw new MethodExtensionConfigurationException(
                    "No ObjectMapper bean found for returnJson conversion on " + method.toGenericString()
            );
        }
        if (candidates.size() > 1) {
            throw new MethodExtensionConfigurationException(
                    "Multiple ObjectMapper beans found for returnJson conversion on " + method.toGenericString()
            );
        }
        return candidates.getFirst();
    }

    private boolean containsTypeVariable(Type type) {
        if (type instanceof TypeVariable<?>) {
            return true;
        }
        if (type instanceof ParameterizedType parameterizedType) {
            if (containsTypeVariable(parameterizedType.getRawType())) {
                return true;
            }
            for (Type argument : parameterizedType.getActualTypeArguments()) {
                if (containsTypeVariable(argument)) {
                    return true;
                }
            }
        }
        if (type instanceof GenericArrayType genericArrayType) {
            return containsTypeVariable(genericArrayType.getGenericComponentType());
        }
        if (type instanceof WildcardType wildcardType) {
            for (Type upperBound : wildcardType.getUpperBounds()) {
                if (containsTypeVariable(upperBound)) {
                    return true;
                }
            }
            for (Type lowerBound : wildcardType.getLowerBounds()) {
                if (containsTypeVariable(lowerBound)) {
                    return true;
                }
            }
        }
        return false;
    }
}
