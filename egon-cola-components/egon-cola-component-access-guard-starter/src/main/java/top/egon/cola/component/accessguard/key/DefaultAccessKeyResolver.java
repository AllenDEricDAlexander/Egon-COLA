package top.egon.cola.component.accessguard.key;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.BeanUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardProperties;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.support.AopMethodResolver;

import java.lang.reflect.Field;
import java.lang.reflect.Executable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

public class DefaultAccessKeyResolver implements AccessKeyResolver, ExecutableAccessKeyResolver {

    private static final String ALL_KEY = "all";

    private final AccessGuardProperties.KeyResolveFailureStrategy blankKeyStrategy;

    private final ParameterNameDiscoverer parameterNameDiscoverer;

    private final AopMethodResolver methodResolver;

    private final AccessGuardKeyGenerator keyGenerator;

    public DefaultAccessKeyResolver() {
        this(AccessGuardProperties.KeyResolveFailureStrategy.USE_ALL);
    }

    public DefaultAccessKeyResolver(AccessGuardProperties.KeyResolveFailureStrategy blankKeyStrategy) {
        this(blankKeyStrategy, new DefaultParameterNameDiscoverer(), new AopMethodResolver(), new AccessGuardKeyGenerator());
    }

    DefaultAccessKeyResolver(
            AccessGuardProperties.KeyResolveFailureStrategy blankKeyStrategy,
            ParameterNameDiscoverer parameterNameDiscoverer,
            AopMethodResolver methodResolver,
            AccessGuardKeyGenerator keyGenerator
    ) {
        this.blankKeyStrategy = blankKeyStrategy;
        this.parameterNameDiscoverer = parameterNameDiscoverer;
        this.methodResolver = methodResolver;
        this.keyGenerator = keyGenerator;
    }

    @Override
    public AccessKeyResolution resolve(ProceedingJoinPoint joinPoint, AccessGuardRule rule) {
        return resolve(methodResolver.resolve(joinPoint), joinPoint.getArgs(), rule);
    }

    public AccessKeyResolution resolve(Method method, Object[] args, AccessGuardRule rule) {
        return resolve((Executable) method, args, rule);
    }

    @Override
    public AccessKeyResolution resolve(
            Executable executable,
            Object[] arguments,
            AccessGuardRule rule
    ) {
        Object[] args = arguments == null ? new Object[0] : arguments;
        String key = StringUtils.hasText(rule.keyExpression()) ? rule.keyExpression() : rule.key();
        if (!StringUtils.hasText(key) || ALL_KEY.equalsIgnoreCase(key.trim())) {
            return keyGenerator.generate(ALL_KEY);
        }

        Object value = resolveValue(executable, args, key.trim());
        String rawKey = Objects.toString(value, "");
        String normalizedKey = rawKey.trim();
        if (!StringUtils.hasText(normalizedKey)) {
            return handleBlankKey(rule);
        }
        return keyGenerator.generate(normalizedKey);
    }

    private Object resolveValue(Executable executable, Object[] args, String key) {
        if (key.startsWith("header:")) {
            return resolveHeader(key.substring("header:".length()));
        }
        if ("ip".equalsIgnoreCase(key)) {
            return resolveIp();
        }

        String[] parameterNames = executable instanceof Method method
                ? parameterNameDiscoverer.getParameterNames(method)
                : parameterNameDiscoverer.getParameterNames((Constructor<?>) executable);
        if (parameterNames != null) {
            Object namedValue = resolveFromNamedParameters(parameterNames, args, key);
            if (namedValue != null) {
                return namedValue;
            }
        }
        if (args.length == 1) {
            return resolveFromSingleArgument(
                    args[0], executable.getParameterTypes()[0], key);
        }
        return null;
    }

    private Object resolveFromNamedParameters(String[] parameterNames, Object[] args, String key) {
        for (int i = 0; i < parameterNames.length && i < args.length; i++) {
            String parameterName = parameterNames[i];
            if (key.equals(parameterName)) {
                return args[i];
            }
            String prefix = parameterName + ".";
            if (key.startsWith(prefix)) {
                return readPropertyPath(args[i], key.substring(prefix.length()));
            }
        }
        return null;
    }

    private Object resolveFromSingleArgument(
            Object argument,
            Class<?> parameterType,
            String key
    ) {
        if (!key.contains(".")) {
            if (BeanUtils.isSimpleValueType(parameterType)) {
                return argument;
            }
            Object propertyValue = readProperty(argument, key);
            return propertyValue != null ? propertyValue : argument;
        }
        return readPropertyPath(argument, key);
    }

    private Object readPropertyPath(Object root, String path) {
        Object current = root;
        for (String segment : path.split("\\.")) {
            current = readProperty(current, segment);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private Object readProperty(Object target, String name) {
        if (target == null || !StringUtils.hasText(name)) {
            return null;
        }
        if (target instanceof Map<?, ?> map) {
            return map.get(name);
        }

        Method accessor = findAccessor(target.getClass(), name);
        if (accessor != null) {
            try {
                return accessor.invoke(target);
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }

        Field field = findField(target.getClass(), name);
        if (field == null) {
            return null;
        }
        try {
            if (!field.trySetAccessible()) {
                return null;
            }
            return field.get(target);
        } catch (IllegalAccessException | RuntimeException ignored) {
            return null;
        }
    }

    private Method findAccessor(Class<?> type, String name) {
        for (String methodName : new String[]{name, "get" + capitalize(name), "is" + capitalize(name)}) {
            try {
                Method method = type.getMethod(methodName);
                if (method.getParameterCount() == 0) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private String resolveHeader(String headerName) {
        Object request = currentRequest();
        if (request == null) {
            return "";
        }
        return invokeString(request, "getHeader", headerName);
    }

    private String resolveIp() {
        Object request = currentRequest();
        if (request == null) {
            return "";
        }
        String forwardedFor = invokeString(request, "getHeader", "X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return invokeString(request, "getRemoteAddr");
    }

    private Object currentRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return null;
        }
        try {
            return requestAttributes.getClass().getMethod("getRequest").invoke(requestAttributes);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private String invokeString(Object target, String methodName, String... args) {
        try {
            Class<?>[] argumentTypes = new Class<?>[args.length];
            Object[] arguments = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                argumentTypes[i] = String.class;
                arguments[i] = args[i];
            }
            Object value = target.getClass().getMethod(methodName, argumentTypes).invoke(target, arguments);
            return value == null ? "" : value.toString();
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    private AccessKeyResolution handleBlankKey(AccessGuardRule rule) {
        if (blankKeyStrategy == AccessGuardProperties.KeyResolveFailureStrategy.REJECT) {
            throw new IllegalArgumentException("Access key resolved blank for rule " + rule.name());
        }
        return keyGenerator.generate(ALL_KEY);
    }

    private String capitalize(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
