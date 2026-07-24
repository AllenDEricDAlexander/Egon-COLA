package top.egon.cola.component.outbox.aop;

import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.DataBindingPropertyAccessor;
import org.springframework.expression.spel.support.ReflectiveMethodExecutor;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.exception.OutboxMessageResolutionException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class OutboxMessageExpressionResolver {

    private static final Set<Class<?>> FORBIDDEN_DECLARING_TYPES = Set.of(
            Object.class,
            Class.class,
            ClassLoader.class,
            Runtime.class,
            System.class,
            Thread.class,
            ProcessBuilder.class
    );

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ConcurrentMap<String, Expression> expressions = new ConcurrentHashMap<>();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer =
            new DefaultParameterNameDiscoverer();
    private final MethodResolver methodResolver = new RestrictedMethodResolver();

    public OutboxMessage resolve(
            String source,
            Method method,
            Object[] arguments,
            Object result
    ) {
        try {
            Expression expression = expressions.computeIfAbsent(source, parser::parseExpression);
            MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                    null,
                    method,
                    arguments,
                    parameterNameDiscoverer
            );
            context.setVariable("result", result);
            context.setTypeLocator(typeName -> {
                throw new EvaluationException("Type access is disabled");
            });
            context.setConstructorResolvers(List.of());
            context.setMethodResolvers(List.of(methodResolver));
            context.setPropertyAccessors(List.of(
                    DataBindingPropertyAccessor.forReadOnlyAccess()
            ));
            Object resolved = expression.getValue(context);
            if (!(resolved instanceof OutboxMessage message)) {
                throw new OutboxMessageResolutionException(
                        "Transactional outbox message expression did not resolve to a message"
                );
            }
            return message;
        } catch (OutboxMessageResolutionException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new OutboxMessageResolutionException(
                    "Failed to resolve transactional outbox message expression",
                    exception
            );
        }
    }

    private static final class RestrictedMethodResolver implements MethodResolver {

        @Override
        public MethodExecutor resolve(
                EvaluationContext context,
                Object targetObject,
                String name,
                List<TypeDescriptor> argumentTypes
        ) throws AccessException {
            if (targetObject == null
                    || targetObject instanceof Class<?>
                    || !argumentTypes.isEmpty()) {
                throw new AccessException("Method access is not permitted");
            }
            Method method = Arrays.stream(targetObject.getClass().getMethods())
                    .filter(candidate -> candidate.getName().equals(name))
                    .filter(candidate -> candidate.getParameterCount() == 0)
                    .filter(candidate -> Modifier.isPublic(candidate.getModifiers()))
                    .filter(candidate -> !Modifier.isStatic(candidate.getModifiers()))
                    .filter(candidate -> isAllowedDeclaringType(candidate.getDeclaringClass()))
                    .findFirst()
                    .orElseThrow(() -> new AccessException("Method access is not permitted"));
            return new ReflectiveMethodExecutor(method);
        }

        private boolean isAllowedDeclaringType(Class<?> declaringType) {
            return !FORBIDDEN_DECLARING_TYPES.contains(declaringType)
                    && !declaringType.getName().startsWith("java.lang.reflect.");
        }
    }
}
