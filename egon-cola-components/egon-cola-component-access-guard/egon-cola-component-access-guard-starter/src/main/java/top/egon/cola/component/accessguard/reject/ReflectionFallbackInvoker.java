package top.egon.cola.component.accessguard.reject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.util.StringUtils;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.context.AccessGuardContext;
import top.egon.cola.component.accessguard.exception.AccessGuardRejectResponseException;
import top.egon.cola.component.accessguard.support.AopMethodResolver;

import java.lang.reflect.Method;
import java.util.Arrays;

public class ReflectionFallbackInvoker implements RejectResponseInvoker {

    private static final String DEFAULT_STRING_REJECT_BODY = "access rejected";

    private final JsonRejectResponseParser jsonRejectResponseParser;

    private final AopMethodResolver methodResolver;

    public ReflectionFallbackInvoker() {
        this(new JsonRejectResponseParser());
    }

    public ReflectionFallbackInvoker(JsonRejectResponseParser jsonRejectResponseParser) {
        this(jsonRejectResponseParser, new AopMethodResolver());
    }

    ReflectionFallbackInvoker(JsonRejectResponseParser jsonRejectResponseParser, AopMethodResolver methodResolver) {
        this.jsonRejectResponseParser = jsonRejectResponseParser;
        this.methodResolver = methodResolver;
    }

    @Override
    public Object reject(ProceedingJoinPoint joinPoint, AccessGuardRule rule, AccessGuardContext context, Object[] args) {
        return reject(joinPoint.getTarget(), methodResolver.resolve(joinPoint), rule, context, args);
    }

    public Object reject(Object target, Method method, AccessGuardRule rule, AccessGuardContext context, Object[] args) {
        if (StringUtils.hasText(rule.fallbackMethod())) {
            Method fallbackMethod = findFallbackMethod(target.getClass(), rule.fallbackMethod(), method.getParameterTypes(), true);
            if (fallbackMethod == null) {
                fallbackMethod = findFallbackMethod(target.getClass(), rule.fallbackMethod(), method.getParameterTypes(), false);
            }
            if (fallbackMethod != null) {
                return invokeFallback(target, fallbackMethod, context, args);
            }
        }

        if (StringUtils.hasText(rule.returnJson())) {
            return jsonRejectResponseParser.parse(rule.returnJson(), method.getReturnType());
        }
        if (method.getReturnType() == String.class) {
            return DEFAULT_STRING_REJECT_BODY;
        }
        return null;
    }

    private Method findFallbackMethod(Class<?> targetType, String fallbackMethodName, Class<?>[] originalParameterTypes, boolean sameArgsOnly) {
        Class<?>[] parameterTypes = sameArgsOnly ? originalParameterTypes : appendContextType(originalParameterTypes);
        Method method = findDeclaredMethod(targetType, fallbackMethodName, parameterTypes);
        if (method != null) {
            return method;
        }
        if (!sameArgsOnly) {
            return findDeclaredMethod(targetType, fallbackMethodName);
        }
        return null;
    }

    private Object invokeFallback(Object target, Method fallbackMethod, AccessGuardContext context, Object[] args) {
        try {
            fallbackMethod.setAccessible(true);
            if (fallbackMethod.getParameterCount() == args.length + 1
                    && fallbackMethod.getParameterTypes()[fallbackMethod.getParameterCount() - 1] == AccessGuardContext.class) {
                Object[] fallbackArgs = Arrays.copyOf(args, args.length + 1);
                fallbackArgs[fallbackArgs.length - 1] = context;
                return fallbackMethod.invoke(target, fallbackArgs);
            }
            if (fallbackMethod.getParameterCount() == 0) {
                return fallbackMethod.invoke(target);
            }
            return fallbackMethod.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new AccessGuardRejectResponseException("Failed to invoke access guard fallback method " + fallbackMethod.getName(), e);
        }
    }

    private Method findDeclaredMethod(Class<?> targetType, String name, Class<?>... parameterTypes) {
        Class<?> current = targetType;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private Class<?>[] appendContextType(Class<?>[] parameterTypes) {
        Class<?>[] result = Arrays.copyOf(parameterTypes, parameterTypes.length + 1);
        result[result.length - 1] = AccessGuardContext.class;
        return result;
    }
}
